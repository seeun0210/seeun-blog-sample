package site.seeun.blogsample.exposed

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class LessonRepositoryTest {
    private val repository = LessonRepository()

    @BeforeTest
    fun setUp() {
        Database.connect(
            url = "jdbc:h2:mem:lesson_sample;MODE=MySQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = "",
        )

        transaction {
            SchemaUtils.create(LessonsTable)
            LessonsTable.deleteAll()
        }
    }

    @Test
    fun `find lessons by course id without generated q type`() {
        transaction {
            repository.save(
                Lesson(
                    id = "lesson-2",
                    courseId = "course-1",
                    title = "Second lesson",
                    sequence = 2,
                ),
            )
            repository.save(
                Lesson(
                    id = "lesson-1",
                    courseId = "course-1",
                    title = "First lesson",
                    sequence = 1,
                ),
            )
            repository.save(
                Lesson(
                    id = "lesson-3",
                    courseId = "course-2",
                    title = "Other course",
                    sequence = 1,
                ),
            )

            val result = repository.findByCourseId("course-1")

            assertEquals(listOf("lesson-1", "lesson-2"), result.map { it.id })
        }
    }
}
