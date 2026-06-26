package site.seeun.blogsample.temporal

/**
 * 결정적 분석은 직접 계산(숫자 소유), 서술 섹션은 [ReportAiClient] 로 위임(문장만).
 * 이 경계가 "숫자는 코드, 문장은 AI" 원칙을 강제한다 — AI는 점수를 만들지 않는다.
 */
class ReportGenerationActivitiesImpl(
    private val ai: ReportAiClient,
) : ReportGenerationActivities {
    override fun analyzeScores(command: ReportCommand): ScoreAnalysis {
        val scores = command.rawScores
        require(scores.isNotEmpty()) { "rawScores must not be empty" }
        return ScoreAnalysis(
            average = scores.average(),
            max = scores.max(),
            min = scores.min(),
        )
    }

    override fun writeSummary(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection =
        NarrativeSection(
            title = "총평",
            body =
                ai.write(
                    instruction = "${command.targetMajor} 지원자의 성적 총평을 2문장으로 작성",
                    context = "평균 ${analysis.average}, 최고 ${analysis.max}, 최저 ${analysis.min}",
                ),
        )

    override fun writeStrength(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection =
        NarrativeSection(
            title = "강점",
            body = ai.write(instruction = "강점 한 가지를 한 문장으로", context = "최고 점수 ${analysis.max}"),
        )

    override fun writeAdvice(
        command: ReportCommand,
        analysis: ScoreAnalysis,
    ): NarrativeSection =
        NarrativeSection(
            title = "보완점",
            body = ai.write(instruction = "보완점 한 가지를 한 문장으로", context = "최저 점수 ${analysis.min}"),
        )
}
