package site.seeun.blogsample.temporal

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface ReportGenerationWorkflow {
    @WorkflowMethod
    fun generate(command: ReportCommand): DiagnosticReport
}
