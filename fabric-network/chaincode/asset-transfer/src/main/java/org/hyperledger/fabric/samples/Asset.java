package org.hyperledger.fabric.samples;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

/**
 * Asset represents a single asset stored on the ledger.
 * Fields are immutable after creation to match ledger semantics.
 */
@DataType()
public final class Asset {

    @Property()
    private final String assetId;

    @Property()
    private final String color;

    @Property()
    private final int size;

    @Property()
    private final String owner;

    @Property()
    private final int appraisedValue;

    public Asset(
            @JsonProperty("assetId") final String assetId,
            @JsonProperty("color") final String color,
            @JsonProperty("size") final int size,
            @JsonProperty("owner") final String owner,
            @JsonProperty("appraisedValue") final int appraisedValue) {
        this.assetId = assetId;
        this.color = color;
        this.size = size;
        this.owner = owner;
        this.appraisedValue = appraisedValue;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getColor() {
        return color;
    }

    public int getSize() {
        return size;
    }

    public String getOwner() {
        return owner;
    }

    public int getAppraisedValue() {
        return appraisedValue;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Asset)) return false;
        Asset other = (Asset) obj;
        return assetId.equals(other.assetId)
                && color.equals(other.color)
                && size == other.size
                && owner.equals(other.owner)
                && appraisedValue == other.appraisedValue;
    }

    @Override
    public int hashCode() {
        int result = assetId.hashCode();
        result = 31 * result + color.hashCode();
        result = 31 * result + size;
        result = 31 * result + owner.hashCode();
        result = 31 * result + appraisedValue;
        return result;
    }

    @Override
    public String toString() {
        return "Asset{" +
                "assetId='" + assetId + '\'' +
                ", color='" + color + '\'' +
                ", size=" + size +
                ", owner='" + owner + '\'' +
                ", appraisedValue=" + appraisedValue +
                '}';
    }
}
