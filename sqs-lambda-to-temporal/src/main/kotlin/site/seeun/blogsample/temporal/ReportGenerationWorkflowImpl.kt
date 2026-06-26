package site.seeun.blogsample.temporal

import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Async
import io.temporal.workflow.Promise
import io.temporal.workflow.Workflow
import java.time.Duration

/**
 * 리포트 생성 오케스트레이션.
 *
 * SQS+Lambda 시절에는 이 흐름이 outbox relay, SQS, 외부 생성기, SNS, consumer, event handler,
 * 4개 processor, 여러 service로 흩어져 있어 "리포트가 어떻게 만들어지는가"를 한눈에 볼 수 없었다.
 * Temporal 워크플로에서는 한 메서드가 흐름 전체를 보여준다.
 *
 * - 결정적 분석 1회 → 서술 3섹션 병렬 fan-out → 배리어 → 조립
 * - 유료 AI 호출이므로 재시도는 maxAttempts=3 + 지수 백오프로 비용을 묶는다(기본은 무제한 재시도).
 */
class ReportGenerationWorkflowImpl : ReportGenerationWorkflow {
    private val activities =
        Workflow.newActivityStub(
            ReportGenerationActivities::class.java,
            ActivityOptions
                .newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(2))
                .setRetryOptions(
                    RetryOptions
                        .newBuilder()
                        .setInitialInterval(Duration.ofSeconds(2))
                        .setBackoffCoefficient(2.0)
                        .setMaximumInterval(Duration.ofSeconds(30))
                        .setMaximumAttempts(3)
                        .build(),
                ).build(),
        )

    override fun generate(command: ReportCommand): DiagnosticReport {
        // 1) 결정적 분석 — 숫자는 여기서 확정된다.
        val analysis = activities.analyzeScores(command)

        // 2) 서술 섹션은 서로 독립이므로 병렬로 시작한다(fan-out).
        val summary = Async.function { activities.writeSummary(command, analysis) }
        val strength = Async.function { activities.writeStrength(command, analysis) }
        val advice = Async.function { activities.writeAdvice(command, analysis) }

        // 3) 배리어에서 모두 모은 뒤 조립한다.
        Promise.allOf(summary, strength, advice).get()
        return DiagnosticReport(
            reportId = command.reportId,
            analysis = analysis,
            summary = summary.get(),
            strength = strength.get(),
            advice = advice.get(),
        )
    }
}
