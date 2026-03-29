package org.hyperledger.fabric.samples;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * AssetTransfer chaincode for Hyperledger Fabric 2.x.
 *
 * Supports full CRUD operations on Asset objects plus:
 *  - range queries (GetAllAssets)
 *  - ownership transfer (TransferAsset)
 *  - existence check (AssetExists)
 *  - ledger initialization with sample data (InitLedger)
 */
@Contract(
        name = "AssetTransfer",
        info = @Info(
                title = "Asset Transfer",
                description = "Asset management chaincode for a 2-org Fabric network",
                version = "1.0.0",
                license = @License(name = "Apache-2.0"),
                contact = @Contact(email = "admin@example.com", name = "Fabric Admin")))
@Default
public final class AssetTransfer implements ContractInterface {

    private final Genson genson = new Genson();

    private enum AssetTransferErrors {
        ASSET_NOT_FOUND,
        ASSET_ALREADY_EXISTS
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────────

    /**
     * Populates the ledger with a set of sample assets.
     * Typically called once during chaincode instantiation / first invoke.
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void InitLedger(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        Asset[] sampleAssets = {
            new Asset("asset1", "blue",   5, "Tomoko", 300),
            new Asset("asset2", "red",    5, "Brad",   400),
            new Asset("asset3", "green", 10, "Jin Soo", 500),
            new Asset("asset4", "yellow", 10, "Max",   600),
            new Asset("asset5", "black", 15, "Adriana", 700),
            new Asset("asset6", "white", 15, "Michel", 800),
        };

        for (Asset asset : sampleAssets) {
            String assetJSON = genson.serialize(asset);
            stub.putStringState(asset.getAssetId(), assetJSON);
            System.out.printf("Asset %s initialized%n", asset.getAssetId());
        }
    }

    // ─── Create ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new asset and stores it on the ledger.
     *
     * @param ctx            transaction context
     * @param assetId        unique identifier
     * @param color          color attribute
     * @param size           size in units
     * @param owner          owner name
     * @param appraisedValue monetary value
     * @return the newly created Asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset CreateAsset(final Context ctx,
                              final String assetId,
                              final String color,
                              final int size,
                              final String owner,
                              final int appraisedValue) {

        ChaincodeStub stub = ctx.getStub();

        if (AssetExists(ctx, assetId)) {
            String errorMessage = String.format("Asset %s already exists", assetId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_ALREADY_EXISTS.toString());
        }

        Asset asset = new Asset(assetId, color, size, owner, appraisedValue);
        String assetJSON = genson.serialize(asset);
        stub.putStringState(assetId, assetJSON);

        // Emit a chaincode event so external applications can subscribe
        stub.setEvent("CreateAsset", assetJSON.getBytes());

        return asset;
    }

    // ─── Read ───────────────────────────────────────────────────────────────────

    /**
     * Retrieves a single asset by ID.
     *
     * @param ctx     transaction context
     * @param assetId asset identifier
     * @return the Asset stored under assetId
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Asset ReadAsset(final Context ctx, final String assetId) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetId);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        return genson.deserialize(assetJSON, Asset.class);
    }

    // ─── Update ─────────────────────────────────────────────────────────────────

    /**
     * Overwrites all fields of an existing asset.
     *
     * @param ctx            transaction context
     * @param assetId        existing asset ID
     * @param color          new color
     * @param size           new size
     * @param owner          new owner
     * @param appraisedValue new appraised value
     * @return the updated Asset
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Asset UpdateAsset(final Context ctx,
                              final String assetId,
                              final String color,
                              final int size,
                              final String owner,
                              final int appraisedValue) {

        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetId)) {
            String errorMessage = String.format("Asset %s does not exist", assetId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset updatedAsset = new Asset(assetId, color, size, owner, appraisedValue);
        String assetJSON = genson.serialize(updatedAsset);
        stub.putStringState(assetId, assetJSON);

        stub.setEvent("UpdateAsset", assetJSON.getBytes());

        return updatedAsset;
    }

    // ─── Delete ─────────────────────────────────────────────────────────────────

    /**
     * Deletes an asset from the ledger. The history is preserved.
     *
     * @param ctx     transaction context
     * @param assetId asset to delete
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void DeleteAsset(final Context ctx, final String assetId) {
        ChaincodeStub stub = ctx.getStub();

        if (!AssetExists(ctx, assetId)) {
            String errorMessage = String.format("Asset %s does not exist", assetId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        stub.delState(assetId);
        stub.setEvent("DeleteAsset", assetId.getBytes());
    }

    // ─── Transfer ───────────────────────────────────────────────────────────────

    /**
     * Transfers asset ownership to a new owner and returns the old owner name.
     *
     * @param ctx      transaction context
     * @param assetId  asset to transfer
     * @param newOwner new owner name
     * @return the old owner name
     */
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String TransferAsset(final Context ctx,
                                 final String assetId,
                                 final String newOwner) {

        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetId);

        if (assetJSON == null || assetJSON.isEmpty()) {
            String errorMessage = String.format("Asset %s does not exist", assetId);
            System.out.println(errorMessage);
            throw new ChaincodeException(errorMessage, AssetTransferErrors.ASSET_NOT_FOUND.toString());
        }

        Asset asset = genson.deserialize(assetJSON, Asset.class);
        String oldOwner = asset.getOwner();

        Asset transferredAsset = new Asset(
                asset.getAssetId(),
                asset.getColor(),
                asset.getSize(),
                newOwner,
                asset.getAppraisedValue());

        String updatedJSON = genson.serialize(transferredAsset);
        stub.putStringState(assetId, updatedJSON);
        stub.setEvent("TransferAsset", updatedJSON.getBytes());

        return oldOwner;
    }

    // ─── Query Helpers ───────────────────────────────────────────────────────────

    /**
     * Checks whether an asset exists in the world state.
     *
     * @param ctx     transaction context
     * @param assetId asset identifier
     * @return true if asset exists
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean AssetExists(final Context ctx, final String assetId) {
        ChaincodeStub stub = ctx.getStub();
        String assetJSON = stub.getStringState(assetId);
        return (assetJSON != null && !assetJSON.isEmpty());
    }

    /**
     * Returns all assets found in the world state using a range query.
     * In a production system consider using rich (CouchDB) queries instead.
     *
     * @param ctx transaction context
     * @return JSON array of all assets
     */
    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllAssets(final Context ctx) {
        ChaincodeStub stub = ctx.getStub();

        List<Asset> queryResults = new ArrayList<>();

        // Empty string range = full table scan
        QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "");

        for (KeyValue result : results) {
            Asset asset = genson.deserialize(result.getStringValue(), Asset.class);
            System.out.println(asset);
            queryResults.add(asset);
        }

        return genson.serialize(queryResults);
    }
}
