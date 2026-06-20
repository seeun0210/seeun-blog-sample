package site.seeun.sample.domain.catalog

data class CatalogCourse(
    val id: String,
    val title: String,
    val price: Long,
)

class CatalogService {
    fun publishCourse(id: String, title: String, price: Long): CatalogCourse {
        require(price >= 0) { "price must be non-negative" }
        return CatalogCourse(id = id, title = title, price = price)
    }
}

