package site.seeun.sample.apps.api

import site.seeun.sample.domain.billing.BillingPort
import site.seeun.sample.domain.billing.PaymentRequest
import site.seeun.sample.domain.catalog.CatalogService
import site.seeun.sample.domain.learning.CourseId
import site.seeun.sample.domain.learning.LearningService
import site.seeun.sample.domain.learning.StudentId

class ApiApplication(
    private val learningService: LearningService,
    private val catalogService: CatalogService,
    private val billingPort: BillingPort,
) {
    fun enrollAndBill(studentId: String, courseId: String, price: Long): String {
        val course = catalogService.publishCourse(courseId, title = "Sample course", price = price)
        learningService.enroll(StudentId(studentId), CourseId(course.id))

        return billingPort
            .charge(PaymentRequest(orderId = "${studentId}-${course.id}", amount = course.price))
            .value
    }
}

