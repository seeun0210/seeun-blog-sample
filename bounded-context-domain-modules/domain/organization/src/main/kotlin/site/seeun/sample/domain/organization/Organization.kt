package site.seeun.sample.domain.organization

data class OrganizationId(val value: String)

class OrganizationService {
    fun canAccessBackoffice(adminId: String, organizationId: OrganizationId): Boolean {
        return adminId.isNotBlank() && organizationId.value.isNotBlank()
    }
}

