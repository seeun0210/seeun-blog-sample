package site.seeun.sample.support.persistence

import site.seeun.sample.domain.learning.Enrollment
import site.seeun.sample.domain.learning.EnrollmentRepository
import site.seeun.sample.domain.learning.StudentId

class InMemoryEnrollmentRepository : EnrollmentRepository {
    private val enrollments = mutableListOf<Enrollment>()

    override fun save(enrollment: Enrollment) {
        enrollments += enrollment
    }

    override fun findByStudent(studentId: StudentId): List<Enrollment> {
        return enrollments.filter { it.studentId == studentId }
    }
}

