#!/usr/bin/env node

import { mkdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";

const root = path.dirname(new URL(import.meta.url).pathname);
const loadRows = (await readFile(path.join(root, "raw/load-traffic.csv"), "utf8"))
  .trim()
  .split("\n")
  .slice(1)
  .map((line) => {
    const [timestamp, elapsedMs, phase, requestId, status, latencyMs, error] = line.split(",");
    return {
      timestamp,
      elapsedMs: Number(elapsedMs),
      phase,
      requestId: Number(requestId),
      status: Number(status),
      latencyMs: Number(latencyMs),
      error,
    };
  });

const benchmarkRows = (await readFile(path.join(root, "raw/results.csv"), "utf8"))
  .trim()
  .split("\n")
  .slice(1)
  .map((line) => {
    const values = line.split(",");
    return {
      round: Number(values[0]),
      condition: values[2],
      readyMs: Number(values[5]),
      firstMs: Number(values[10]),
      memoryMiB: Number(values[16]),
    };
  });

const percentile = (values, ratio) => {
  const sorted = [...values].sort((left, right) => left - right);
  return sorted[Math.min(sorted.length - 1, Math.ceil(sorted.length * ratio) - 1)];
};
const median = (values) => percentile(values, 0.5);
const format = (value) => Number(value).toFixed(3);
const csv = (headers, rows) => [headers.join(","), ...rows.map((row) => row.join(","))].join("\n");

const successfulRows = loadRows.filter((row) => row.status >= 200 && row.status < 300);
const runStartedAt = Date.parse(loadRows[0].timestamp) - loadRows[0].elapsedMs;
const runEndedAt = Math.max(...loadRows.map((row) => Date.parse(row.timestamp)));
const buckets = new Map();
for (const row of successfulRows) {
  const second = Math.floor(row.elapsedMs / 1000);
  const bucket = buckets.get(second) ?? [];
  bucket.push(row.latencyMs);
  buckets.set(second, bucket);
}

const latencyCsv = csv(
  ["Time", "Latency"],
  successfulRows.map((row) => [row.timestamp, format(row.latencyMs)]),
);
const rateCsv = csv(
  ["Time", "Requests/sec"],
  [...buckets.entries()].map(([second, values]) => [new Date(runStartedAt + second * 1000).toISOString(), values.length]),
);
const percentileCsv = csv(
  ["Time", "p50", "p95", "p99"],
  [...buckets.entries()].map(([second, values]) => [
    new Date(runStartedAt + second * 1000).toISOString(),
    format(percentile(values, 0.5)),
    format(percentile(values, 0.95)),
    format(percentile(values, 0.99)),
  ]),
);

const timestampForRound = (round) => new Date(runStartedAt + (round * 10 - 5) * 1000).toISOString();
const byRound = new Map();
for (const row of benchmarkRows) {
  const value = byRound.get(row.round) ?? {};
  value[row.condition] = row;
  byRound.set(row.round, value);
}
const datasource = { type: "grafana-testdata-datasource", uid: "warmup-testdata" };
const firstRequestTargets = [
  target(
    csv(
      ["Time", "Value"],
      [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.control.firstMs)]),
    ),
    "A",
    "Before · no runtime warmup",
  ),
  target(
    csv(
      ["Time", "Value"],
      [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.business.firstMs)]),
    ),
    "B",
    "After · business warmup",
  ),
];
const readyTargets = [
  target(csv(["Time", "Value"], [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.control.readyMs / 1000)])), "A", "Before · no runtime warmup"),
  target(csv(["Time", "Value"], [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.business.readyMs / 1000)])), "B", "After · business warmup"),
];
const memoryTargets = [
  target(csv(["Time", "Value"], [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.control.memoryMiB)])), "A", "Before · no runtime warmup"),
  target(csv(["Time", "Value"], [...byRound.entries()].map(([round, conditions]) => [timestampForRound(round), format(conditions.business.memoryMiB)])), "B", "After · business warmup"),
];

const controlFirstMedian = median(benchmarkRows.filter((row) => row.condition === "control").map((row) => row.firstMs));
const businessFirstMedian = median(benchmarkRows.filter((row) => row.condition === "business").map((row) => row.firstMs));
const firstReduction = (1 - businessFirstMedian / controlFirstMedian) * 100;
const controlWarmP50 = 17.115;
const businessWarmP50 = 16.202;
const controlColdPenalty = controlFirstMedian - controlWarmP50;
const businessColdPenalty = businessFirstMedian - businessWarmP50;
const coldPenaltyReduction = (1 - businessColdPenalty / controlColdPenalty) * 100;
const overallLatency = successfulRows.map((row) => row.latencyMs);

const thresholds = { mode: "absolute", steps: [{ color: "green", value: null }] };
const colors = [
  {
    matcher: { id: "byName", options: "Before · no runtime warmup" },
    properties: [{ id: "color", value: { fixedColor: "#F2495C", mode: "fixed" } }],
  },
  {
    matcher: { id: "byName", options: "After · business warmup" },
    properties: [{ id: "color", value: { fixedColor: "#73BF69", mode: "fixed" } }],
  },
];

function target(content, refId = "A", alias = "") {
  return {
  datasource,
  refId,
  scenarioId: "csv_content",
  csvContent: content,
    alias,
  };
}

const timeSeries = ({ id, title, x, y, w, h, content, targets, unit, legend = true, overrides = [], fillOpacity = 8 }) => ({
  id,
  title,
  type: "timeseries",
  datasource,
  gridPos: { x, y, w, h },
  fieldConfig: {
    defaults: {
      color: { mode: "palette-classic" },
      custom: {
        axisBorderShow: false,
        axisCenteredZero: false,
        axisColorMode: "text",
        axisLabel: "",
        axisPlacement: "auto",
        barAlignment: 0,
        barWidthFactor: 0.6,
        drawStyle: "line",
        fillOpacity,
        gradientMode: "none",
        hideFrom: { legend: false, tooltip: false, viz: false },
        insertNulls: false,
        lineInterpolation: "smooth",
        lineWidth: 1,
        pointSize: 4,
        scaleDistribution: { type: "linear" },
        showPoints: "never",
        spanNulls: false,
        stacking: { group: "A", mode: "none" },
        thresholdsStyle: { mode: "off" },
      },
      mappings: [],
      thresholds,
      unit,
    },
    overrides,
  },
  options: {
    legend: { calcs: ["lastNotNull", "max"], displayMode: "table", placement: "bottom", showLegend: legend },
    tooltip: { hideZeros: false, mode: "multi", sort: "desc" },
  },
  targets: targets ?? [target(content)],
});

const stat = ({ id, title, x, y, w = 4, value, unit, decimals = 1, color = "#73BF69" }) => ({
  id,
  title,
  type: "stat",
  datasource,
  gridPos: { x, y, w, h: 4 },
  fieldConfig: {
    defaults: {
      color: { mode: "fixed", fixedColor: color },
      decimals,
      mappings: [],
      thresholds,
      unit,
    },
    overrides: [],
  },
  options: {
    colorMode: "value",
    graphMode: "area",
    justifyMode: "auto",
    orientation: "auto",
    percentChangeColorMode: "standard",
    reduceOptions: { calcs: ["lastNotNull"], fields: "", values: false },
    showPercentChange: false,
    textMode: "auto",
    wideLayout: true,
  },
  targets: [target(csv(["Time", "Value"], [[new Date(runStartedAt).toISOString(), value]]))],
});

const dashboard = {
  uid: "jvm-warmup-local",
  title: "JVM Business-path Warmup · Local Load Test",
  tags: ["jvm", "warmup", "jdk25", "local-load-test"],
  timezone: "browser",
  schemaVersion: 42,
  version: 1,
  refresh: "",
  time: {
    from: new Date(runStartedAt - 3000).toISOString(),
    to: new Date(runEndedAt + 3000).toISOString(),
  },
  timepicker: {},
  annotations: { list: [] },
  templating: { list: [] },
  panels: [
    stat({ id: 1, title: "First request · Before", x: 0, y: 0, value: controlFirstMedian, unit: "ms", color: "#F2495C" }),
    stat({ id: 2, title: "First request · After", x: 4, y: 0, value: businessFirstMedian, unit: "ms", color: "#73BF69" }),
    stat({ id: 3, title: "First request · 개선", x: 8, y: 0, value: firstReduction, unit: "percent", color: "#73BF69" }),
    stat({ id: 4, title: "Cold penalty · Before", x: 12, y: 0, value: controlColdPenalty, unit: "ms", color: "#F2495C" }),
    stat({ id: 5, title: "Cold penalty · After", x: 16, y: 0, value: businessColdPenalty, unit: "ms", color: "#73BF69" }),
    stat({ id: 6, title: "Cold penalty · 개선", x: 20, y: 0, value: coldPenaltyReduction, unit: "percent", color: "#73BF69" }),
    stat({ id: 13, title: "실제 로컬 요청", x: 0, y: 4, w: 6, value: loadRows.length, unit: "none", decimals: 0, color: "#5794F2" }),
    stat({ id: 14, title: "성공률", x: 6, y: 4, w: 6, value: (successfulRows.length / loadRows.length) * 100, unit: "percent", color: "#73BF69" }),
    stat({ id: 15, title: "부하 테스트 p99", x: 12, y: 4, w: 6, value: percentile(overallLatency, 0.99), unit: "ms", color: "#FADE2A" }),
    stat({ id: 16, title: "Peak throughput", x: 18, y: 4, w: 6, value: Math.max(...[...buckets.values()].map((values) => values.length)), unit: "reqps", decimals: 0, color: "#FF9830" }),
    timeSeries({ id: 7, title: "응답시간 (2,864 actual local requests)", x: 0, y: 8, w: 16, h: 9, content: latencyCsv, unit: "ms", legend: false, fillOpacity: 4 }),
    timeSeries({ id: 8, title: "요청 처리량", x: 16, y: 8, w: 8, h: 9, content: rateCsv, unit: "reqps", fillOpacity: 24 }),
    timeSeries({ id: 9, title: "응답시간 분위수", x: 0, y: 17, w: 8, h: 9, content: percentileCsv, unit: "ms", fillOpacity: 4 }),
    timeSeries({ id: 10, title: "Before vs After · 재기동별 첫 비즈니스 요청 (n=5)", x: 8, y: 17, w: 8, h: 9, targets: firstRequestTargets, unit: "ms", overrides: colors, fillOpacity: 2 }),
    timeSeries({ id: 11, title: "Readiness 도달 시간", x: 16, y: 17, w: 4, h: 9, targets: readyTargets, unit: "s", overrides: colors, fillOpacity: 2 }),
    timeSeries({ id: 12, title: "요청 후 컨테이너 메모리", x: 20, y: 17, w: 4, h: 9, targets: memoryTargets, unit: "mbytes", overrides: colors, fillOpacity: 2 }),
  ],
};

const textPanel = ({ id, title, x, y, w, h, content, transparent = false }) => ({
  id,
  title,
  type: "text",
  gridPos: { x, y, w, h },
  transparent,
  options: {
    code: { language: "plaintext", showLineNumbers: false, showMiniMap: false },
    content,
    mode: "markdown",
  },
});

const mechanismDashboard = {
  uid: "jvm-warmup-mechanism",
  title: "JDK 25 AOTCache + Business-path Warmup · Runtime Flow",
  tags: ["jvm", "warmup", "jdk25", "mechanism"],
  timezone: "browser",
  schemaVersion: 42,
  version: 1,
  refresh: "",
  time: { from: "now-5m", to: "now" },
  timepicker: {},
  annotations: { list: [] },
  templating: { list: [] },
  panels: [
    textPanel({
      id: 101,
      title: "IMAGE BUILD · AOT TRAINING",
      x: 0,
      y: 0,
      w: 24,
      h: 2,
      content: "",
    }),
    textPanel({ id: 102, title: "① JVM 시작", x: 0, y: 2, w: 4, h: 5, content: "## Bytecode\n\n애플리케이션 JAR과 전체 classpath를 읽는다." }),
    textPanel({ id: 103, title: "→", x: 4, y: 2, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 104, title: "② Training run", x: 5, y: 2, w: 4, h: 5, content: "## `/v3/api-docs × 100`\n\n이 경로에서 실제로 쓰인 class와 method profile만 관찰된다." }),
    textPanel({ id: 105, title: "→", x: 9, y: 2, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 106, title: "③ JEP 483", x: 10, y: 2, w: 4, h: 5, content: "## Class state\n\n읽기·파싱·로딩·링킹이 끝난 class 상태를 저장한다." }),
    textPanel({ id: 107, title: "+", x: 14, y: 2, w: 1, h: 5, content: "# +", transparent: true }),
    textPanel({ id: 108, title: "④ JEP 515", x: 15, y: 2, w: 4, h: 5, content: "## Method profile\n\n호출 빈도·분기·타입 profile을 저장해 JIT의 출발점을 앞당긴다." }),
    textPanel({ id: 109, title: "→", x: 19, y: 2, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 110, title: "⑤ AOT cache", x: 20, y: 2, w: 4, h: 5, content: "## `app.aot`\n\n다음 실행에서 `-XX:AOTCache=app.aot`로 로드한다.\n\n**JIT 컴파일 비용 자체는 남는다.**" }),

    textPanel({ id: 111, title: "CONTROL · BUSINESS PATH NOT TRAINED", x: 0, y: 7, w: 24, h: 2, content: "" }),
    textPanel({ id: 112, title: "① AOT load", x: 0, y: 9, w: 4, h: 5, content: "## `app.aot` 로드\n\n문서 경로에서 학습한 class state와 method profile을 복원한다." }),
    textPanel({ id: 113, title: "→", x: 4, y: 9, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 114, title: "② Spring", x: 5, y: 9, w: 4, h: 5, content: "## Context 시작\n\n빈 생성과 서버 바인딩을 마친다." }),
    textPanel({ id: 115, title: "→", x: 9, y: 9, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 116, title: "③ Readiness", x: 10, y: 9, w: 4, h: 5, content: "## `UP`\n\n비즈니스 경로를 실행하지 않은 채 외부 트래픽을 받는다." }),
    textPanel({ id: 117, title: "→", x: 14, y: 9, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 118, title: "④ First user", x: 15, y: 9, w: 4, h: 5, content: "## 숨은 초기화\n\nMVC route · Jackson · DB mapping · branch/type profile · JIT compilation" }),
    textPanel({ id: 119, title: "=", x: 19, y: 9, w: 1, h: 5, content: "# =", transparent: true }),
    textPanel({ id: 120, title: "첫 비즈니스 요청", x: 20, y: 9, w: 4, h: 5, content: "# 112.7 ms\n\nWarm p50 17.1 ms\n\nCold penalty **95.6 ms**" }),

    textPanel({ id: 121, title: "BUSINESS-PATH WARMUP · COST MOVED BEFORE TRAFFIC", x: 0, y: 14, w: 24, h: 2, content: "" }),
    textPanel({ id: 122, title: "① Readiness gate", x: 0, y: 16, w: 4, h: 5, content: "## `OUT_OF_SERVICE`\n\nwarmup이 끝날 때까지 로드밸런서 트래픽을 차단한다." }),
    textPanel({ id: 123, title: "→", x: 4, y: 16, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 124, title: "② Internal GET", x: 5, y: 16, w: 4, h: 5, content: "## 실제 비즈니스 API\n\n`GET /teacher-profiles`\n\n중앙값 **490.4 ms**" }),
    textPanel({ id: 125, title: "→", x: 9, y: 16, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 126, title: "③ Runtime warmup", x: 10, y: 16, w: 4, h: 5, content: "## 실제 실행\n\nroute · serializer · DB · profile/JIT 경로를 한 번 통과한다." }),
    textPanel({ id: 127, title: "→", x: 14, y: 16, w: 1, h: 5, content: "# →", transparent: true }),
    textPanel({ id: 128, title: "④ Readiness", x: 15, y: 16, w: 4, h: 5, content: "## `UP`\n\n모든 warmup 요청이 2xx일 때만 트래픽을 연다." }),
    textPanel({ id: 129, title: "=", x: 19, y: 16, w: 1, h: 5, content: "# =", transparent: true }),
    textPanel({ id: 130, title: "첫 사용자 요청", x: 20, y: 16, w: 4, h: 5, content: "# 47.9 ms\n\nWarm p50 16.2 ms\n\nCold penalty **31.7 ms**" }),
  ],
};

const provisioningRoot = path.join(root, "grafana/provisioning");
await mkdir(path.join(provisioningRoot, "datasources"), { recursive: true });
await mkdir(path.join(provisioningRoot, "dashboards"), { recursive: true });
await mkdir(path.join(root, "grafana/dashboards"), { recursive: true });
await writeFile(
  path.join(provisioningRoot, "datasources/testdata.yml"),
  "apiVersion: 1\n\ndatasources:\n  - name: Warmup TestData\n    uid: warmup-testdata\n    type: grafana-testdata-datasource\n    access: proxy\n    isDefault: true\n",
);
await writeFile(
  path.join(provisioningRoot, "dashboards/provider.yml"),
  "apiVersion: 1\n\nproviders:\n  - name: warmup\n    type: file\n    disableDeletion: true\n    updateIntervalSeconds: 10\n    options:\n      path: /var/lib/grafana/dashboards\n      foldersFromFilesStructure: false\n",
);
await writeFile(path.join(root, "grafana/dashboards/jvm-warmup-local.json"), JSON.stringify(dashboard, null, 2));
await writeFile(path.join(root, "grafana/dashboards/jvm-warmup-mechanism.json"), JSON.stringify(mechanismDashboard, null, 2));
await writeFile(
  path.join(root, "grafana/dashboard-meta.json"),
  JSON.stringify(
    {
      from: runStartedAt - 3000,
      to: runEndedAt + 3000,
      loadRequests: loadRows.length,
      successfulRequests: successfulRows.length,
      firstRequestReductionPercent: Number(firstReduction.toFixed(1)),
      coldPenaltyReductionPercent: Number(coldPenaltyReduction.toFixed(1)),
    },
    null,
    2,
  ),
);

console.log(`Grafana dashboard generated: ${path.join(root, "grafana/dashboards/jvm-warmup-local.json")}`);
