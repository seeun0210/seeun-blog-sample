#!/usr/bin/env node

import { performance } from "node:perf_hooks";
import { writeFile } from "node:fs/promises";

const [targetUrl, outputPath] = process.argv.slice(2);
if (!targetUrl || !outputPath) {
  throw new Error("usage: node generate-load.mjs <url> <output.csv>");
}

const phases = [
  { name: "baseline", durationSeconds: 12, requestsPerSecond: 12 },
  { name: "morning-rise", durationSeconds: 12, requestsPerSecond: 45 },
  { name: "campaign-burst", durationSeconds: 8, requestsPerSecond: 110 },
  { name: "settle", durationSeconds: 12, requestsPerSecond: 25 },
  { name: "notification-burst", durationSeconds: 8, requestsPerSecond: 90 },
  { name: "steady", durationSeconds: 8, requestsPerSecond: 35 },
];

let randomState = 20260818;
function random() {
  randomState = (randomState * 1664525 + 1013904223) >>> 0;
  return randomState / 2 ** 32;
}

const schedule = [];
let phaseStartMs = 0;
let requestId = 1;
for (const phase of phases) {
  const intervalMs = 1000 / phase.requestsPerSecond;
  const count = phase.durationSeconds * phase.requestsPerSecond;
  for (let index = 0; index < count; index += 1) {
    const jitterMs = (random() - 0.5) * intervalMs * 0.8;
    schedule.push({
      requestId: requestId++,
      phase: phase.name,
      scheduledMs: phaseStartMs + (index + 0.5) * intervalMs + jitterMs,
    });
  }
  phaseStartMs += phase.durationSeconds * 1000;
}

const runStartedAt = Date.now();
const monotonicStart = performance.now();

const results = await Promise.all(
  schedule.map(
    (item) =>
      new Promise((resolve) => {
        const delayMs = Math.max(0, item.scheduledMs - (performance.now() - monotonicStart));
        setTimeout(async () => {
          const startedAt = Date.now();
          const requestStarted = performance.now();
          try {
            const response = await fetch(targetUrl, {
              headers: { accept: "application/json", "user-agent": "warmup-local-load-test/1.0" },
              signal: AbortSignal.timeout(5000),
            });
            await response.arrayBuffer();
            resolve({
              ...item,
              startedAt,
              status: response.status,
              latencyMs: performance.now() - requestStarted,
              error: "",
            });
          } catch (error) {
            resolve({
              ...item,
              startedAt,
              status: 0,
              latencyMs: performance.now() - requestStarted,
              error: error instanceof Error ? error.name : "UnknownError",
            });
          }
        }, delayMs);
      }),
  ),
);

results.sort((left, right) => left.startedAt - right.startedAt || left.requestId - right.requestId);
const rows = ["timestamp,elapsed_ms,phase,request_id,status,latency_ms,error"];
for (const result of results) {
  rows.push(
    [
      new Date(result.startedAt).toISOString(),
      result.startedAt - runStartedAt,
      result.phase,
      result.requestId,
      result.status,
      result.latencyMs.toFixed(3),
      result.error,
    ].join(","),
  );
}
await writeFile(outputPath, `${rows.join("\n")}\n`, "utf8");

const successful = results.filter((result) => result.status >= 200 && result.status < 300);
const sortedLatencies = successful.map((result) => result.latencyMs).sort((a, b) => a - b);
const percentile = (ratio) => sortedLatencies[Math.min(sortedLatencies.length - 1, Math.ceil(sortedLatencies.length * ratio) - 1)];
const durationSeconds = (Math.max(...results.map((result) => result.startedAt)) - runStartedAt) / 1000;

console.log(
  JSON.stringify(
    {
      requests: results.length,
      successful: successful.length,
      durationSeconds: Number(durationSeconds.toFixed(3)),
      averageRps: Number((results.length / durationSeconds).toFixed(1)),
      p50Ms: Number(percentile(0.5).toFixed(3)),
      p95Ms: Number(percentile(0.95).toFixed(3)),
      p99Ms: Number(percentile(0.99).toFixed(3)),
      maxMs: Number(sortedLatencies.at(-1).toFixed(3)),
    },
    null,
    2,
  ),
);
