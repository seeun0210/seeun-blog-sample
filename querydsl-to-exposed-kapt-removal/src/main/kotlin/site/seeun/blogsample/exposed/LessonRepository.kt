package site.seeun.blogsample.exposed

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

class LessonRepository {
    fun save(lesson: Lesson) {
        LessonsTable.insert {
            it[id] = lesson.id
            it[courseId] = lesson.courseId
            it[title] = lesson.title
            it[sequence] = lesson.sequence
        }
    }

    fun findByCourseId(courseId: String): List<Lesson> =
        LessonsTable
            .selectAll()
            .where { LessonsTable.courseId eq courseId }
            .orderBy(LessonsTable.sequence to SortOrder.ASC)
            .map { row -> row.toLesson() }
}
