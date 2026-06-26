package site.seeun.blogsample.temporal

import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.temporal.client.WorkflowClientOptions
import io.temporal.client.WorkflowOptions
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.testing.TestEnvironmentOptions
import io.temporal.testing.TestWorkflowEnvironment
import io.temporal.worker.Worker
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TestWorkflowEnvironment 로 실제 Temporal 서버 없이 워크플로를 검증한다.
 * AI는 결정적 가짜 구현으로 대체 — 오케스트레이션(결정적 분석 + 병렬 서술 + 조립)만 본다.
 */
class ReportGenerationWorkflowTest {
    private val taskQueue = "report-generation"
    private lateinit var env: TestWorkflowEnvironment

    @BeforeTest
    fun setUp() {
        env =
            TestWorkflowEnvironment.newInstance(
                TestEnvironmentOptions
                    .newBuilder()
                    .setWorkflowClientOptions(
                        WorkflowClientOptions.newBuilder().setDataConverter(kotlinDataConverter()).build(),
                    ).build(),
            )
        val worker: Worker = env.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(ReportGenerationWorkflowImpl::class.java)
        // 결정적 가짜 AI: instruction/context를 그대로 엮어 돌려준다.
        val fakeAi = ReportAiClient { instruction, context -> "[$instruction] $context" }
        worker.registerActivitiesImplementations(ReportGenerationActivitiesImpl(fakeAi))
        env.start()
    }

    @AfterTest
    fun tearDown() {
        env.close()
    }

    @Test
    fun `결정적 분석과 병렬 서술을 조립해 리포트를 만든다`() {
        val workflow =
            env.workflowClient.newWorkflowStub(
                ReportGenerationWorkflow::class.java,
                WorkflowOptions.newBuilder().setTaskQueue(taskQueue).build(),
            )

        val report =
            workflow.generate(
                ReportCommand(
                    reportId = "r-1",
                    studentName = "홍길동",
                    targetMajor = "컴퓨터공학과",
                    rawScores = listOf(90, 80, 100),
                ),
            )

        assertEquals("r-1", report.reportId)
        assertEquals(90.0, report.analysis.average)
        assertEquals(100, report.analysis.max)
        assertEquals(80, report.analysis.min)
        assertEquals("총평", report.summary.title)
        assertEquals("강점", report.strength.title)
        assertEquals("보완점", report.advice.title)
    }

    /** Temporal 기본 DataConverter는 Kotlin data class를 역직렬화하지 못한다 → Jackson Kotlin module 등록. */
    private fun kotlinDataConverter(): DataConverter {
        val mapper = JacksonJsonPayloadConverter.newDefaultObjectMapper().registerKotlinModule()
        return DefaultDataConverter
            .newDefaultInstance()
            .withPayloadConverterOverrides(JacksonJsonPayloadConverter(mapper))
    }
}
