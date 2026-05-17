package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * StatusListRecord — W3C Status List 2021 snapshot stored on Fabric ledger.
 *
 * Key format: "statuslist:{listId}"
 *
 * Stores the encoded bitstring (base64url-encoded gzip-compressed bitmap)
 * representing the revocation state of every VC referencing this list.
 * Each VC issued by the backend contains:
 *   credentialStatus = {
 *     id: ".../status-list/{listId}#{index}",
 *     type: "StatusList2021Entry",
 *     statusPurpose: "revocation",
 *     statusListIndex: "{index}",
 *     statusListCredential: ".../status-list/{listId}"
 *   }
 *
 * Verifier resolves the statusListCredential URL, decodes the bitstring,
 * checks the bit at statusListIndex. 0 = active, 1 = revoked.
 *
 * The full bitstring (not individual entries) is stored to preserve audit history.
 */
@DataType()
public final class StatusListRecord {

    @Property()
    private final String listId;

    @Property()
    private final String encodedList;

    @Property()
    private final long size;

    @Property()
    private final long updatedIndex;

    @Property()
    private final boolean revoked;

    @Property()
    private final String updatedAt;

    @Property()
    private final String updatedBy;

    public StatusListRecord(
            @JsonProperty("listId")       final String listId,
            @JsonProperty("encodedList")  final String encodedList,
            @JsonProperty("size")         final long size,
            @JsonProperty("updatedIndex") final long updatedIndex,
            @JsonProperty("revoked")      final boolean revoked,
            @JsonProperty("updatedAt")    final String updatedAt,
            @JsonProperty("updatedBy")    final String updatedBy) {
        this.listId       = listId;
        this.encodedList  = encodedList;
        this.size         = size;
        this.updatedIndex = updatedIndex;
        this.revoked      = revoked;
        this.updatedAt    = updatedAt;
        this.updatedBy    = updatedBy;
    }

    public String  getListId()       { return listId; }
    public String  getEncodedList()  { return encodedList; }
    public long    getSize()         { return size; }
    public long    getUpdatedIndex() { return updatedIndex; }
    public boolean getRevoked()      { return revoked; }
    public String  getUpdatedAt()    { return updatedAt; }
    public String  getUpdatedBy()    { return updatedBy; }

    @Override
    public String toString() {
        return "StatusListRecord{listId='" + listId +
               "', size=" + size +
               ", updatedIndex=" + updatedIndex +
               ", revoked=" + revoked +
               ", updatedAt='" + updatedAt + "'}";
    }
}
