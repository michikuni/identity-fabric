package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * DIDDocument — W3C-inspired DID Document stored on Fabric ledger.
 *
 * Key format: "did:{did}"
 *
 * On-chain (public, immutable):
 *   - did          = "did:fabric:trustid:<employeeCode>"
 *   - employeeId   = reference back to employee record
 *   - publicKeyJwk = ECDSA P-256 public key in JWK format (JSON string)
 *   - controller   = DID of the issuing organization (Org1)
 *   - status       = ACTIVE | REVOKED
 *   - createdAt    = ISO-8601 UTC
 *   - updatedAt    = ISO-8601 UTC
 *   - revokedAt    = ISO-8601 UTC (null if not revoked)
 *   - revokedBy    = actor who revoked (null if not revoked)
 *   - revokeReason = reason string (null if not revoked)
 */
@DataType()
public final class DIDDocument {

    @Property()
    private final String did;

    @Property()
    private final String employeeId;

    @Property()
    private final String publicKeyJwk;

    @Property()
    private final String controller;

    @Property()
    private final String status;

    @Property()
    private final String createdAt;

    @Property()
    private final String updatedAt;

    @Property()
    private final String revokedAt;

    @Property()
    private final String revokedBy;

    @Property()
    private final String revokeReason;

    public DIDDocument(
            @JsonProperty("did")          final String did,
            @JsonProperty("employeeId")   final String employeeId,
            @JsonProperty("publicKeyJwk") final String publicKeyJwk,
            @JsonProperty("controller")   final String controller,
            @JsonProperty("status")       final String status,
            @JsonProperty("createdAt")    final String createdAt,
            @JsonProperty("updatedAt")    final String updatedAt,
            @JsonProperty("revokedAt")    final String revokedAt,
            @JsonProperty("revokedBy")    final String revokedBy,
            @JsonProperty("revokeReason") final String revokeReason) {
        this.did          = did;
        this.employeeId   = employeeId;
        this.publicKeyJwk = publicKeyJwk;
        this.controller   = controller;
        this.status       = status;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
        this.revokedAt    = revokedAt;
        this.revokedBy    = revokedBy;
        this.revokeReason = revokeReason;
    }

    public String getDid()          { return did; }
    public String getEmployeeId()   { return employeeId; }
    public String getPublicKeyJwk() { return publicKeyJwk; }
    public String getController()   { return controller; }
    public String getStatus()       { return status; }
    public String getCreatedAt()    { return createdAt; }
    public String getUpdatedAt()    { return updatedAt; }
    public String getRevokedAt()    { return revokedAt; }
    public String getRevokedBy()    { return revokedBy; }
    public String getRevokeReason() { return revokeReason; }

    @Override
    public String toString() {
        return "DIDDocument{did='" + did +
               "', employeeId='" + employeeId +
               "', status='" + status +
               "', createdAt='" + createdAt + "'}";
    }
}
