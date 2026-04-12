package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * IdentityRecord — the unit stored on the Fabric ledger.
 *
 * Design: Partial data + hash (privacy-first approach).
 *
 * On-chain (public, immutable):
 *   - recordId       = "profile:emp-123"
 *   - employeeId     = unique employee identifier
 *   - recordType     = PROFILE | CONTRACT | PAYROLL
 *   - status         = ACTIVE | REVOKED | DELETED
 *   - keyFields      = JSON of non-sensitive key fields
 *   - dataHash       = SHA-256 hash of full off-chain data (proof of integrity)
 *   - action         = CREATE | UPDATE | DELETE
 *   - timestamp      = ISO-8601 UTC
 *   - updatedBy      = who made the change (employeeId of actor)
 *
 * Off-chain (in MySQL / IPFS):
 *   - Full profile (PII: phone, address, identity number...)
 *   - Files, images, PDFs
 */
@DataType()
public final class IdentityRecord {

    @Property()
    private final String recordId;

    @Property()
    private final String employeeId;

    @Property()
    private final String recordType;

    @Property()
    private final String status;

    /** JSON string containing non-sensitive summarized fields. */
    @Property()
    private final String keyFields;

    /** SHA-256 hex digest of the full off-chain data payload. */
    @Property()
    private final String dataHash;

    @Property()
    private final String action;

    @Property()
    private final String timestamp;

    @Property()
    private final String updatedBy;

    public IdentityRecord(
            @JsonProperty("recordId")   final String recordId,
            @JsonProperty("employeeId") final String employeeId,
            @JsonProperty("recordType") final String recordType,
            @JsonProperty("status")     final String status,
            @JsonProperty("keyFields")  final String keyFields,
            @JsonProperty("dataHash")   final String dataHash,
            @JsonProperty("action")     final String action,
            @JsonProperty("timestamp")  final String timestamp,
            @JsonProperty("updatedBy")  final String updatedBy) {
        this.recordId   = recordId;
        this.employeeId = employeeId;
        this.recordType = recordType;
        this.status     = status;
        this.keyFields  = keyFields;
        this.dataHash   = dataHash;
        this.action     = action;
        this.timestamp  = timestamp;
        this.updatedBy  = updatedBy;
    }

    public String getRecordId()   { return recordId; }
    public String getEmployeeId() { return employeeId; }
    public String getRecordType() { return recordType; }
    public String getStatus()     { return status; }
    public String getKeyFields()  { return keyFields; }
    public String getDataHash()   { return dataHash; }
    public String getAction()     { return action; }
    public String getTimestamp()  { return timestamp; }
    public String getUpdatedBy()  { return updatedBy; }

    @Override
    public String toString() {
        return "IdentityRecord{recordId='" + recordId +
               "', employeeId='" + employeeId +
               "', recordType='" + recordType +
               "', status='" + status +
               "', action='" + action +
               "', timestamp='" + timestamp + "'}";
    }
}
