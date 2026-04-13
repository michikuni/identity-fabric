package com.mpcorp.identity.infrastructures.fabric

import com.fasterxml.jackson.databind.ObjectMapper
import com.mpcorp.identity.domain.entity.ContractEntity
import com.mpcorp.identity.domain.entity.PayrollEntity
import com.mpcorp.identity.domain.entity.ProfileEntity
import org.fabric.api.model.UpsertIdentityRecordRequest
import org.fabric.api.service.IdentityLedgerService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.security.MessageDigest

/**
 * FabricLedgerBridge — cầu nối giữa com.mpcorp.identity và org.fabric.api.
 *
 * Thay thế IdentityLedgerService cũ (cái gọi Gateway trực tiếp).
 * Giờ delegate toàn bộ việc ghi blockchain sang [IdentityLedgerService] của org.fabric.api.
 *
 * Strategy: Fire-and-forget (async).
 *   - MySQL là source of truth.
 *   - Blockchain ghi audit trail + hash proof.
 *   - Nếu Fabric lỗi → log warning, KHÔNG rollback MySQL.
 *
 * Được inject vào các UseCase của com.mpcorp.identity (CreateProfileUseCase, UpdateProfileUseCase, ...).
 */
@Service
class FabricLedgerBridge(
    private val ledgerService: IdentityLedgerService,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(FabricLedgerBridge::class.java)

    // ── Profile ───────────────────────────────────────────────────────────────

    @Async
    fun upsertProfileRecord(profile: ProfileEntity, action: String = "CREATE") {
        val employeeId = profile.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] ProfileEntity missing employeeId, skip")
            return
        }
        runCatching {
            val keyFields = objectMapper.writeValueAsString(
                mapOf(
                    "name"           to profile.name,
                    "gender"         to profile.gender,
                    "educationLevel" to profile.educationLevel,
                    "major"          to profile.major,
                    "expYears"       to profile.expYears,
                    "email"          to profile.email,
                )
            )
            val fullJson   = objectMapper.writeValueAsString(profile)
            val dataHash   = sha256(fullJson)
            val status     = if (action == "DELETE") "DELETED" else "ACTIVE"

            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "PROFILE",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] PROFILE record written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write PROFILE record for employeeId=$employeeId — ${ex.message}")
        }
    }

    @Async
    fun deleteProfileRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "PROFILE", updatedBy = "system")
            log.info("[FabricBridge] PROFILE DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write PROFILE DELETE for employeeId=$employeeId — ${ex.message}")
        }
    }

    // ── Contract ──────────────────────────────────────────────────────────────

    @Async
    fun upsertContractRecord(contract: ContractEntity, action: String = "CREATE") {
        val employeeId = contract.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] ContractEntity missing employeeId, skip")
            return
        }
        runCatching {
            val keyFields = objectMapper.writeValueAsString(
                mapOf(
                    "typeContract"   to contract.typeContract,
                    "startDate"      to contract.startDate?.toString(),
                    "endDate"        to contract.endDate?.toString(),
                    "contractExpire" to contract.contractExpire?.toString(),
                )
            )
            val fullJson = objectMapper.writeValueAsString(contract)
            val dataHash = sha256(fullJson)
            val status   = if (action == "DELETE") "DELETED" else "ACTIVE"

            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "CONTRACT",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] CONTRACT record written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write CONTRACT record for employeeId=$employeeId — ${ex.message}")
        }
    }

    @Async
    fun deleteContractRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "CONTRACT", updatedBy = "system")
            log.info("[FabricBridge] CONTRACT DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write CONTRACT DELETE for employeeId=$employeeId — ${ex.message}")
        }
    }

    // ── Payroll ───────────────────────────────────────────────────────────────

    @Async
    fun upsertPayrollRecord(payroll: PayrollEntity, action: String = "CREATE") {
        val employeeId = payroll.employee.id?.toString() ?: run {
            log.warn("[FabricBridge] PayrollEntity missing employeeId, skip")
            return
        }
        runCatching {
            val keyFields = objectMapper.writeValueAsString(
                mapOf(
                    "salaryType"  to payroll.salaryType,
                    "currency"    to payroll.currency,
                    "totalIncome" to payroll.totalIncome,
                    "payDay"      to payroll.payDay?.toString(),
                    "bankName"    to payroll.bankName,
                )
            )
            // Không hash số tài khoản và số lương cụ thể — chỉ hash toàn bộ object
            val fullJson = objectMapper.writeValueAsString(payroll)
            val dataHash = sha256(fullJson)
            val status   = if (action == "DELETE") "DELETED" else "ACTIVE"

            ledgerService.upsertRecord(
                UpsertIdentityRecordRequest(
                    employeeId = employeeId,
                    recordType = "PAYROLL",
                    status     = status,
                    keyFields  = keyFields,
                    dataHash   = dataHash,
                    action     = action,
                    updatedBy  = "system",
                )
            )
            log.info("[FabricBridge] PAYROLL record written — employeeId=$employeeId action=$action")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write PAYROLL record for employeeId=$employeeId — ${ex.message}")
        }
    }

    @Async
    fun deletePayrollRecord(employeeId: String) {
        runCatching {
            ledgerService.deleteRecord(employeeId, "PAYROLL", updatedBy = "system")
            log.info("[FabricBridge] PAYROLL DELETE written — employeeId=$employeeId")
        }.onFailure { ex ->
            log.warn("[FabricBridge] Failed to write PAYROLL DELETE for employeeId=$employeeId — ${ex.message}")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun sha256(data: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}