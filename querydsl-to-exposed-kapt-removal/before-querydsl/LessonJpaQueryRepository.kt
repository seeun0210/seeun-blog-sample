package site.seeun.blogsample.beforequerydsl

import com.querydsl.jpa.impl.JPAQueryFactory

class LessonJpaQueryRepository(
    private val queryFactory: JPAQueryFactory,
) {
    fun findByCourseId(courseId: String): List<LessonJpaEntity> {
        val lesson = QLessonJpaEntity.lessonJpaEntity

        return queryFactory
            .selectFrom(lesson)
            .where(lesson.courseId.eq(courseId))
            .orderBy(lesson.sequence.asc())
            .fetch()
    }
}

class LessonJpaEntity

object QLessonJpaEntity {
    val lessonJpaEntity: QLessonJpaEntity = QLessonJpaEntity()

    val courseId: StringPath = StringPath()
    val sequence: NumberPath = NumberPath()
}

class StringPath {
    fun eq(value: String): BooleanExpression = BooleanExpression(value)
}

class NumberPath {
    fun asc(): OrderSpecifier = OrderSpecifier()
}

class BooleanExpression(
    val value: String,
)

class OrderSpecifier
