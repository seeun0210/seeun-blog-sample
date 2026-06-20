package site.seeun.sample.domain.billing

data class PaymentRequest(
    val orderId: String,
    val amount: Long,
)

data class InvoiceId(val value: String)

interface BillingPort {
    fun charge(request: PaymentRequest): InvoiceId
}

class BillingService : BillingPort {
    override fun charge(request: PaymentRequest): InvoiceId {
        require(request.amount > 0) { "amount must be positive" }
        return InvoiceId("invoice-${request.orderId}")
    }
}

