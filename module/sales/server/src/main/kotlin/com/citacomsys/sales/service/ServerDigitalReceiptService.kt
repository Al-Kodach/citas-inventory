package com.citacomsys.sales.service

import com.citacomsys.api.sales.service.DigitalReceiptService
import com.citacomsys.api.workspace.repository.WorkspaceRepository
import com.citacomsys.core.notification.service.SmsService
import com.citacomsys.types.PhoneNumber
import com.citacomsys.types.jvm.JvmPhoneNumber
import com.citacomsys.types.jvm.toJvm

internal class ServerDigitalReceiptService(
    private val smsService: SmsService,
    private val workspaceRepo: WorkspaceRepository,
    private val saleService: OnlineSaleService
) : DigitalReceiptService {

    override suspend fun sendSmsReceipt(
        saleId: String,
        phone: PhoneNumber,
        workspaceId: String
    ): Result<Unit> = runCatching {
        val validatedPhone = phone.toJvm()

        val saleDetails = saleService.getSaleDetails(
            workspaceId = workspaceId,
            saleId = saleId
        )
        val workspace = workspaceRepo.get(workspaceId).getOrThrow()

        val digitalReceipt = buildString {
            append("${workspace.name}\n")
            append("ID: ${saleId.takeLast(8).uppercase()}\n")
            append("Date: ${saleDetails.sale.createdAt.toString().split("T").first()}\n")
            append("\n")
            saleDetails.items.groupBy { it.productName }.forEach { (name, instances) ->
                val totalQty = instances.sumOf { it.quantity }
                val price = instances.first().unitPrice
                append("$name x $totalQty @ ${price.amount}\n")
            }
            append("\n")
            append("Total: ${saleDetails.sale.totalAmount.currencyCode} ${saleDetails.sale.totalAmount.amount}\n")
            append("Thank you!")
        }

        smsService.send(
            userId = workspaceId,
            organizationId = workspace.organizationId,
            workspaceId = workspaceId,
            to = validatedPhone.e164,
            body = digitalReceipt
        )
    }

    override suspend fun sendReceipt(
        phone: String,
        receipt: String,
        workspaceId: String
    ): Result<Unit> = runCatching {
        val phoneNum = JvmPhoneNumber.create(phone)
        val workspace = workspaceRepo.get(workspaceId).getOrThrow()

        smsService.send(
            userId = workspaceId,
            organizationId = workspace.organizationId,
            workspaceId = workspaceId,
            to = phoneNum.e164,
            body = receipt
        )
    }
}
