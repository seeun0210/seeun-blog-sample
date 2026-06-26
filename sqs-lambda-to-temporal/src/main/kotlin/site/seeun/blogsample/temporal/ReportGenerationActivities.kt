package site.seeun.blogsample.temporal

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

/**
 * 워크플로가 호출하는 액티비티 경계. 결정적 분석 1개 + 서술 섹션 3개.
 * 각 메서드는 재시도·타임아웃이 걸리는 독립 단위다.
 */
@ActivityInterface
interface ReportGenerationActivities {
    @ActivityMethod
    fun analyzeScores(command: ReportCommand): ScoreAnalysis

    @ActivityMethod
    fun writeSummary(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection

    @ActivityMethod
    fun writeStrength(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection

    @ActivityMethod
    fun writeAdvice(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection
}
