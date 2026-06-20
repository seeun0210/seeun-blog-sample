package site.seeun.sample.apps.backoffice

import site.seeun.sample.domain.billing.BillingPort
import site.seeun.sample.domain.billing.PaymentRequest
import site.seeun.sample.domain.organization.OrganizationId
import site.seeun.sample.domain.organization.OrganizationService

class BackofficeApplication(
    private val organizationService: OrganizationService,
    private val billingPort: BillingPort,
) {
    fun chargeOrganization(adminId: String, organizationId: String, amount: Long): String {
        val resolvedOrganizationId = OrganizationId(organizationId)
        check(organizationService.canAccessBackoffice(adminId, resolvedOrganizationId))

        return billingPort
            .charge(PaymentRequest(orderId = organizationId, amount = amount))
            .value
    }
}

