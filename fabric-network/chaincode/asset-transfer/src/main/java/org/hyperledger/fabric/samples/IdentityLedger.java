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
 *   - Employee profiles    (recordType = PROFILE)
 *   - Contracts            (recordType = CONTRACT)
 *   - Payroll records      (recordType = PAYROLL)
 *
 * Key format: "{recordType}:{employeeId}"
 *   e.g. "profile:emp-123", "contract:emp-123"
 *
 * The full sensitive data lives off-chain in MySQL.
 * Blockchain provides: immutability, audit trail, hash verification.
 */
@Contract(
        name = "IdentityLedger",
        info = @Info(
                title = "Identity Ledger",
                description = "Records identity audit trail on Hyperledger Fabric",
                version = "1.0.0",
                license = @License(name = "Apache-2.0"),
                contact = @Contact(email = "admin@mpcorp.com", name = "MpCorp Dev Team")))
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

    // ─── Upsert ───────────────────────────────────────────────────────────────

    /**
     * Creates or updates an identity record on the ledger.
     * This is the primary write operation called from the Spring backend.
     *
     * @param ctx         transaction context
     * @param employeeId  unique employee identifier
     * @param recordType  PROFILE | CONTRACT | PAYROLL
     * @param status      ACTIVE | REVOKED | DELETED
     * @param keyFields   JSON of non-sensitive summary fields
     * @param dataHash    SHA-256 of the full off-chain data
     * @param action      CREATE | UPDATE | DELETE
     * @param timestamp   ISO-8601 UTC timestamp
     * @param updatedBy   actor's employeeId
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
        for (String type : new String[]{"profile", "contract", "payroll"}) {
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
}
