package site.seeun.sample.domain.learning

data class CourseId(val value: String)

data class StudentId(val value: String)

data class Enrollment(
    val courseId: CourseId,
    val studentId: StudentId,
)

interface EnrollmentRepository {
    fun save(enrollment: Enrollment)
    fun findByStudent(studentId: StudentId): List<Enrollment>
}

class LearningService(
    private val enrollmentRepository: EnrollmentRepository,
) {
    fun enroll(studentId: StudentId, courseId: CourseId): Enrollment {
        val enrollment = Enrollment(courseId = courseId, studentId = studentId)
        enrollmentRepository.save(enrollment)
        return enrollment
    }
}

