package site.seeun.blogsample.exposed

import org.jetbrains.exposed.v1.core.ResultRow

fun ResultRow.toLesson(): Lesson =
    Lesson(
        id = this[LessonsTable.id],
        courseId = this[LessonsTable.courseId],
        title = this[LessonsTable.title],
        sequence = this[LessonsTable.sequence],
    )
