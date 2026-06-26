package site.seeun.blogsample.temporal

/** 리포트 생성 입력. 워크플로 한 번 = 리포트 한 건. */
data class ReportCommand(
    val reportId: String,
    val studentName: String,
    val targetMajor: String,
    val rawScores: List<Int>,
)

/** 결정적 분석 결과. 숫자는 전부 여기서 확정된다(코드 소유). */
data class ScoreAnalysis(
    val average: Double,
    val max: Int,
    val min: Int,
)

/** AI가 문장만 채우는 서술 섹션. */
data class NarrativeSection(
    val title: String,
    val body: String,
)

/** 최종 조립 리포트. */
data class DiagnosticReport(
    val reportId: String,
    val analysis: ScoreAnalysis,
    val summary: NarrativeSection,
    val strength: NarrativeSection,
    val advice: NarrativeSection,
)
