package org.hyperledger.fabric.samples;

import com.owlike.genson.Genson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public final class AssetTransferTest {

    private final Genson genson = new Genson();

    // ─── Stub helpers ──────────────────────────────────────────────────────────

    private static final class MockKeyValue implements KeyValue {
        private final String key;
        private final String value;

        MockKeyValue(final String key, final String value) {
            this.key = key;
            this.value = value;
        }

        @Override public String getKey() { return key; }
        @Override public byte[] getValue() { return value.getBytes(); }
        @Override public String getStringValue() { return value; }
    }

    private static final class MockAssetResultsIterator implements QueryResultsIterator<KeyValue> {
        private final List<KeyValue> assetList = new ArrayList<>();

        MockAssetResultsIterator() {
            Genson g = new Genson();
            assetList.add(new MockKeyValue("asset1", g.serialize(new Asset("asset1", "blue",  5, "Tomoko", 300))));
            assetList.add(new MockKeyValue("asset2", g.serialize(new Asset("asset2", "red",   5, "Brad",   400))));
            assetList.add(new MockKeyValue("asset3", g.serialize(new Asset("asset3", "green",10, "Jin Soo",500))));
        }

        @Override public Iterator<KeyValue> iterator() { return assetList.iterator(); }
        @Override public void close() {}
    }

    // ─── InitLedger ────────────────────────────────────────────────────────────

    @Nested
    class InitLedgerTests {
        @Test
        void initLedgerPopulatesState() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            contract.InitLedger(ctx);

            // Verify at least 6 assets were written
            verify(stub, times(6)).putStringState(anyString(), anyString());
        }
    }

    // ─── CreateAsset ───────────────────────────────────────────────────────────

    @Nested
    class CreateAssetTests {
        @Test
        void createNewAsset() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("asset1")).thenReturn("");

            Asset asset = contract.CreateAsset(ctx, "asset1", "blue", 5, "Tomoko", 300);

            assertThat(asset).isEqualTo(new Asset("asset1", "blue", 5, "Tomoko", 300));
            verify(stub).putStringState(eq("asset1"), anyString());
        }

        @Test
        void createDuplicateAssetThrows() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Asset existing = new Asset("asset1", "blue", 5, "Tomoko", 300);
            when(stub.getStringState("asset1")).thenReturn(new Genson().serialize(existing));

            assertThatThrownBy(() -> contract.CreateAsset(ctx, "asset1", "red", 10, "Brad", 500))
                    .isInstanceOf(ChaincodeException.class)
                    .hasMessageContaining("asset1 already exists");
        }
    }

    // ─── ReadAsset ─────────────────────────────────────────────────────────────

    @Nested
    class ReadAssetTests {
        @Test
        void readExistingAsset() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Asset asset = new Asset("asset1", "blue", 5, "Tomoko", 300);
            when(stub.getStringState("asset1")).thenReturn(new Genson().serialize(asset));

            Asset result = contract.ReadAsset(ctx, "asset1");
            assertThat(result).isEqualTo(asset);
        }

        @Test
        void readNonExistentAssetThrows() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("asset99")).thenReturn("");

            assertThatThrownBy(() -> contract.ReadAsset(ctx, "asset99"))
                    .isInstanceOf(ChaincodeException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    // ─── UpdateAsset ───────────────────────────────────────────────────────────

    @Nested
    class UpdateAssetTests {
        @Test
        void updateExistingAsset() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Asset existing = new Asset("asset1", "blue", 5, "Tomoko", 300);
            when(stub.getStringState("asset1")).thenReturn(new Genson().serialize(existing));

            Asset updated = contract.UpdateAsset(ctx, "asset1", "green", 8, "Tomoko", 450);
            assertThat(updated.getColor()).isEqualTo("green");
            assertThat(updated.getAppraisedValue()).isEqualTo(450);
        }

        @Test
        void updateNonExistentAssetThrows() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStringState("asset99")).thenReturn("");

            assertThatThrownBy(() -> contract.UpdateAsset(ctx, "asset99", "blue", 5, "John", 100))
                    .isInstanceOf(ChaincodeException.class)
                    .hasMessageContaining("does not exist");
        }
    }

    // ─── TransferAsset ─────────────────────────────────────────────────────────

    @Nested
    class TransferAssetTests {
        @Test
        void transferAssetChangesOwner() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);

            Asset asset = new Asset("asset1", "blue", 5, "Tomoko", 300);
            when(stub.getStringState("asset1")).thenReturn(new Genson().serialize(asset));

            String oldOwner = contract.TransferAsset(ctx, "asset1", "Brad");
            assertThat(oldOwner).isEqualTo("Tomoko");
        }
    }

    // ─── GetAllAssets ──────────────────────────────────────────────────────────

    @Nested
    class GetAllAssetsTests {
        @Test
        void getAllAssetsReturnsArray() {
            AssetTransfer contract = new AssetTransfer();
            Context ctx = mock(Context.class);
            ChaincodeStub stub = mock(ChaincodeStub.class);
            when(ctx.getStub()).thenReturn(stub);
            when(stub.getStateByRange("", "")).thenReturn(new MockAssetResultsIterator());

            String result = contract.GetAllAssets(ctx);
            assertThat(result).contains("asset1", "asset2", "asset3");
        }
    }
}
