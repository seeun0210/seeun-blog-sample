package site.seeun.blogsample.exposed

import org.jetbrains.exposed.v1.core.Table

object LessonsTable : Table("lessons") {
    val id = varchar("id", 26)
    val courseId = varchar("course_id", 26)
    val title = varchar("title", 120)
    val sequence = integer("sequence")

    override val primaryKey = PrimaryKey(id)
}
