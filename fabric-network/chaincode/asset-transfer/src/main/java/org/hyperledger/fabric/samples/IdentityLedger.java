package org.hyperledger.fabric.samples;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * IdentityLedger chaincode — replaces the sample AssetTransfer.
 *
 * Stores identity audit records (partial data + hash) for:
 * - Employee profiles (recordType = PROFILE)
 * - Contracts (recordType = CONTRACT)
 * - Payroll records (recordType = PAYROLL)
 *
 * Key format: "{recordType}:{employeeId}"
 * e.g. "profile:emp-123", "contract:emp-123"
 *
 * The full sensitive data lives off-chain in MySQL.
 * Blockchain provides: immutability, audit trail, hash verification.
 */
@Contract(name = "IdentityLedger", info = @Info(title = "Identity Ledger", description = "Records identity audit trail on Hyperledger Fabric", version = "1.0.0", license = @License(name = "Apache-2.0"), contact = @Contact(email = "admin@mpcorp.com", name = "MpCorp Dev Team")))
@Default
public final class IdentityLedger implements ContractInterface {

    private final Genson genson = new Genson();

    private enum LedgerErrors {
        RECORD_NOT_FOUND,
        RECORD_ALREADY_EXISTS,
        INVALID_ARGUMENT
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildKey(String recordType, String employeeId) {
        return recordType.toLowerCase() + ":" + employeeId;
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    /**
     * Initializes the ledger. Can be called once during bootstrap.
     * Currently a no-op — records are written by the backend via UpsertRecord.
     * Override this to seed sample data if needed.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        System.out.println("[IdentityLedger] InitLedger called — ledger is ready.");
    }

    // ─── Upsert ───────────────────────────────────────────────────────────────

    /**
     * Creates or updates an identity record on the ledger.
     * This is the primary write operation called from the Spring backend.
     *
     * @param ctx        transaction context
     * @param employeeId unique employee identifier
     * @param recordType PROFILE | CONTRACT | PAYROLL
     * @param status     ACTIVE | REVOKED | DELETED
     * @param keyFields  JSON of non-sensitive summary fields
     * @param dataHash   SHA-256 of the full off-chain data
     * @param action     CREATE | UPDATE | DELETE
     * @param timestamp  ISO-8601 UTC timestamp
     * @param updatedBy  actor's employeeId
     * @return the stored IdentityRecord
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public IdentityRecord UpsertRecord(
            final Context ctx,
            final String employeeId,
            final String recordType,
            final String status,
            final String keyFields,
            final String dataHash,
            final String action,
            final String timestamp,
            final String updatedBy) {

        ChaincodeStub stub = ctx.getStub();
        String key = buildKey(recordType, employeeId);

        IdentityRecord record = new IdentityRecord(
                key, employeeId, recordType, status,
                keyFields, dataHash, action, timestamp, updatedBy);

        stub.putStringState(key, genson.serialize(record));
        stub.setEvent("IdentityRecordUpserted", genson.serialize(record).getBytes());

        System.out.printf("[IdentityLedger] %s %s for employee=%s%n", action, recordType, employeeId);
        return record;
    }

    // ─── Delete (soft) ───────────────────────────────────────────────────────

    /**
     * Soft-deletes an identity record by updating its status to DELETED
     * and action to DELETE. The record remains on the ledger for audit purposes.
     *
     * @param ctx        transaction context
     * @param employeeId unique employee identifier
     * @param recordType PROFILE | CONTRACT | PAYROLL
     * @param updatedBy  actor who requested the deletion
     * @return the updated IdentityRecord with DELETED status
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public IdentityRecord DeleteRecord(
            final Context ctx,
            final String employeeId,
            final String recordType,
            final String updatedBy) {

        ChaincodeStub stub = ctx.getStub();
        String key = buildKey(recordType, employeeId);
        String json = stub.getStringState(key);

        if (json == null || json.isEmpty()) {
            String msg = String.format("Cannot delete: no %s record found for employee %s", recordType, employeeId);
            throw new ChaincodeException(msg, LedgerErrors.RECORD_NOT_FOUND.toString());
        }

        // Read existing record to preserve keyFields and dataHash
        IdentityRecord existing = genson.deserialize(json, IdentityRecord.class);

        String timestamp = java.time.Instant.now().toString();
        IdentityRecord deleted = new IdentityRecord(
                key, employeeId, recordType, "DELETED",
                existing.getKeyFields(), existing.getDataHash(),
                "DELETE", timestamp, updatedBy);

        stub.putStringState(key, genson.serialize(deleted));
        stub.setEvent("IdentityRecordDeleted", genson.serialize(deleted).getBytes());

        System.out.printf("[IdentityLedger] DELETE %s for employee=%s by %s%n", recordType, employeeId, updatedBy);
        return deleted;
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    /**
     * Returns the latest state of a record.
     *
     * @param ctx        transaction context
     * @param recordType PROFILE | CONTRACT | PAYROLL
     * @param employeeId employee identifier
     * @return IdentityRecord or throws if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public IdentityRecord GetRecord(
            final Context ctx,
            final String recordType,
            final String employeeId) {

        ChaincodeStub stub = ctx.getStub();
        String key = buildKey(recordType, employeeId);
        String json = stub.getStringState(key);

        if (json == null || json.isEmpty()) {
            String msg = String.format("No %s record found for employee %s", recordType, employeeId);
            throw new ChaincodeException(msg, LedgerErrors.RECORD_NOT_FOUND.toString());
        }

        return genson.deserialize(json, IdentityRecord.class);
    }

    /**
     * Checks whether a record exists without throwing.
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean RecordExists(
            final Context ctx,
            final String recordType,
            final String employeeId) {

        String json = ctx.getStub().getStringState(buildKey(recordType, employeeId));
        return json != null && !json.isEmpty();
    }

    /**
     * Returns the full history of a record (all versions on ledger).
     * Each entry shows what changed and when.
     *
     * @return JSON array of HistoryEntry objects
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetRecordHistory(
            final Context ctx,
            final String recordType,
            final String employeeId) {

        String key = buildKey(recordType, employeeId);
        QueryResultsIterator<KeyModification> history = ctx.getStub().getHistoryForKey(key);

        List<String> entries = new ArrayList<>();
        for (KeyModification mod : history) {
            entries.add(String.format(
                    "{\"txId\":\"%s\",\"timestamp\":\"%s\",\"isDelete\":%b,\"value\":%s}",
                    mod.getTxId(),
                    mod.getTimestamp().toString(),
                    mod.isDeleted(),
                    mod.getStringValue().isEmpty() ? "null" : mod.getStringValue()));
        }

        return "[" + String.join(",", entries) + "]";
    }

    /**
     * Returns all records for a given employee (profile + contracts + payroll).
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllRecordsByEmployee(final Context ctx, final String employeeId) {
        ChaincodeStub stub = ctx.getStub();
        List<IdentityRecord> results = new ArrayList<>();

        // Scan all known record types for this employee
        for (String type : new String[] { "profile", "contract", "payroll" }) {
            String json = stub.getStringState(type + ":" + employeeId);
            if (json != null && !json.isEmpty()) {
                results.add(genson.deserialize(json, IdentityRecord.class));
            }
        }

        return genson.serialize(results);
    }

    /**
     * Returns all records in the ledger (for admin/audit use).
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllRecords(final Context ctx) {
        QueryResultsIterator<KeyValue> results = ctx.getStub().getStateByRange("", "");
        List<IdentityRecord> records = new ArrayList<>();
        for (KeyValue kv : results) {
            records.add(genson.deserialize(kv.getStringValue(), IdentityRecord.class));
        }
        return genson.serialize(records);
    }

    // ─── DID Layer ───────────────────────────────────────────────────────────

    /**
     * Registers a new DID Document on the ledger.
     * Called by the backend when Admin approves an employee account.
     *
     * @param ctx          transaction context
     * @param did          DID string, e.g. "did:fabric:trustid:EMP-2024-001"
     * @param employeeId   reference to the employee record
     * @param publicKeyJwk ECDSA P-256 public key in JWK JSON format
     * @param controller   DID of the issuing org, e.g. "did:fabric:trustid:org1"
     * @param timestamp    ISO-8601 UTC creation timestamp
     * @return the stored DIDDocument
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DIDDocument RegisterDID(
            final Context ctx,
            final String did,
            final String employeeId,
            final String publicKeyJwk,
            final String controller,
            final String timestamp) {

        ChaincodeStub stub = ctx.getStub();

        if (did == null || did.isEmpty() || publicKeyJwk == null || publicKeyJwk.isEmpty()) {
            throw new ChaincodeException("did and publicKeyJwk are required", LedgerErrors.INVALID_ARGUMENT.toString());
        }

        String key = "did:" + did;
        String existing = stub.getStringState(key);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("DID already registered: " + did, LedgerErrors.RECORD_ALREADY_EXISTS.toString());
        }

        DIDDocument doc = new DIDDocument(
                did, employeeId, publicKeyJwk, controller,
                "ACTIVE", timestamp, timestamp, null, null, null);

        stub.putStringState(key, genson.serialize(doc));
        stub.setEvent("DIDRegistered", genson.serialize(doc).getBytes());

        System.out.printf("[IdentityLedger] RegisterDID did=%s employee=%s%n", did, employeeId);
        return doc;
    }

    /**
     * Revokes a DID Document. Called when an employee contract is terminated.
     * The record is updated (not deleted) — full history is preserved.
     *
     * @param ctx         transaction context
     * @param did         DID string to revoke
     * @param revokedBy   actor performing the revocation (Admin/Chief employeeId)
     * @param revokeReason reason string, e.g. "CONTRACT_TERMINATED"
     * @param timestamp   ISO-8601 UTC revocation timestamp
     * @return the updated DIDDocument with status=REVOKED
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public DIDDocument RevokeDID(
            final Context ctx,
            final String did,
            final String revokedBy,
            final String revokeReason,
            final String timestamp) {

        ChaincodeStub stub = ctx.getStub();
        String key = "did:" + did;
        String json = stub.getStringState(key);

        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("DID not found: " + did, LedgerErrors.RECORD_NOT_FOUND.toString());
        }

        DIDDocument existing = genson.deserialize(json, DIDDocument.class);

        if ("REVOKED".equals(existing.getStatus())) {
            throw new ChaincodeException("DID already revoked: " + did, LedgerErrors.INVALID_ARGUMENT.toString());
        }

        DIDDocument revoked = new DIDDocument(
                existing.getDid(), existing.getEmployeeId(), existing.getPublicKeyJwk(),
                existing.getController(), "REVOKED",
                existing.getCreatedAt(), timestamp,
                timestamp, revokedBy, revokeReason);

        stub.putStringState(key, genson.serialize(revoked));
        stub.setEvent("DIDRevoked", genson.serialize(revoked).getBytes());

        System.out.printf("[IdentityLedger] RevokeDID did=%s by=%s reason=%s%n", did, revokedBy, revokeReason);
        return revoked;
    }

    /**
     * Resolves a DID to its DID Document.
     * Used by Verifiers (banks, other employers) to check key material and status.
     *
     * @param ctx transaction context
     * @param did DID string to resolve
     * @return DIDDocument or throws if not found
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public DIDDocument ResolveDID(final Context ctx, final String did) {
        String key = "did:" + did;
        String json = ctx.getStub().getStringState(key);

        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("DID not found: " + did, LedgerErrors.RECORD_NOT_FOUND.toString());
        }

        return genson.deserialize(json, DIDDocument.class);
    }

    // ─── Verify hash integrity ────────────────────────────────────────────────

    /**
     * Verifies the integrity of off-chain data by comparing the provided hash
     * against the dataHash stored on-chain.
     *
     * Workflow:
     *   1. Backend computes SHA-256 of current MySQL data
     *   2. Calls VerifyRecord with that hash
     *   3. Chaincode compares against immutable on-chain dataHash
     *   4. Returns { valid, reason, recordId, storedHash, providedHash, timestamp }
     *
     * @param ctx           transaction context
     * @param recordType    PROFILE | CONTRACT | PAYROLL
     * @param employeeId    employee identifier
     * @param hashToVerify  SHA-256 hex computed from current off-chain data
     * @return JSON verification result
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String VerifyRecord(
            final Context ctx,
            final String recordType,
            final String employeeId,
            final String hashToVerify) {

        String key = buildKey(recordType, employeeId);
        String json = ctx.getStub().getStringState(key);

        if (json == null || json.isEmpty()) {
            return String.format(
                "{\"valid\":false,\"reason\":\"Record not found for %s:%s\","
                + "\"recordId\":\"%s\",\"storedHash\":\"\",\"providedHash\":\"%s\",\"timestamp\":\"\"}",
                recordType, employeeId, key, hashToVerify);
        }

        IdentityRecord record = genson.deserialize(json, IdentityRecord.class);
        String storedHash = record.getDataHash() != null ? record.getDataHash() : "";
        boolean valid = storedHash.equalsIgnoreCase(hashToVerify);
        String reason = valid
            ? "Hash matches — data integrity confirmed"
            : "Hash mismatch — off-chain data may have been tampered with";

        return String.format(
            "{\"valid\":%b,\"reason\":\"%s\",\"recordId\":\"%s\","
            + "\"storedHash\":\"%s\",\"providedHash\":\"%s\",\"timestamp\":\"%s\"}",
            valid, reason, record.getRecordId(),
            storedHash, hashToVerify, record.getTimestamp());
    }
}
