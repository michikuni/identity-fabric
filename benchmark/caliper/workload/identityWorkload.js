'use strict';

// identityWorkload.js — Workload module dùng chung cho mọi giao dịch chaincode TrustID.
// Tham số sinh ra ĐÃ KHỚP chữ ký thật của chaincode identity-ledger
// (đối chiếu IdentityLedgerService.kt của backend — client thật đang chạy).
//
//   - contractId: tên chaincode (identity-ledger)
//   - func:       tên hàm chaincode
//   - kind:       'submit' (ghi) hoặc 'evaluate' (đọc)
//   - keyPrefix:  tiền tố key (giữ để tương thích cấu hình cũ)
//
// Giao dịch GHI dùng key DUY NHẤT mỗi tx → tránh xung đột MVCC, lấy throughput sạch.

const { WorkloadModuleBase } = require('@hyperledger/caliper-core');

const DUMMY_HASH = 'a'.repeat(64);
const DUMMY_JWK = JSON.stringify({
    kty: 'EC', crv: 'P-256',
    x: 'f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU',
    y: 'x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0',
});

class IdentityWorkload extends WorkloadModuleBase {
    constructor() {
        super();
        this.txIndex = 0;
    }

    async initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext) {
        await super.initializeWorkloadModule(workerIndex, totalWorkers, roundIndex, roundArguments, sutAdapter, sutContext);
        this.contractId = roundArguments.contractId || 'identity-ledger';
        this.func = roundArguments.func;
        this.kind = roundArguments.kind || 'submit';
        this.keyPrefix = roundArguments.keyPrefix || 'rec';

        // GetRecord / GetRecordHistory cần record có sẵn → seed 1 record bằng UpsertRecord (8 tham số đúng).
        if (this.func === 'GetRecord' || this.func === 'GetRecordHistory') {
            this.seedEmployeeId = `seed-w${this.workerIndex}`;
            this.seedRecordType = 'PROFILE';
            await this.sutAdapter.sendRequests({
                contractId: this.contractId,
                contractFunction: 'UpsertRecord',
                invokerIdentity: 'User1',
                contractArguments: [
                    this.seedEmployeeId, this.seedRecordType, 'ACTIVE', '{"name":"seed"}',
                    DUMMY_HASH, 'CREATE', new Date().toISOString(), 'caliper-seed',
                ],
                readOnly: false,
            });
        }
    }

    _genArgs() {
        const w = this.workerIndex;
        const i = this.txIndex;
        const ts = new Date().toISOString();
        switch (this.func) {
            case 'RegisterDID':
                // (did, employeeId, publicKeyJwk, controller, timestamp)
                return [`did:fabric:trustid:bench-${w}-${i}`, `bench-${w}-${i}`, DUMMY_JWK, 'did:fabric:trustid:org1', ts];
            case 'UpsertRecord':
                // (employeeId, recordType, status, keyFields, dataHash, action, timestamp, updatedBy)
                return [`bench-${w}-${i}`, 'PROFILE', 'ACTIVE', '{"name":"bench"}', DUMMY_HASH, 'CREATE', ts, 'caliper'];
            case 'UpdateStatusListEntry':
                // (listId, encodedList, size, updatedIndex, revoked, timestamp, updatedBy) — key unique/tx tránh MVCC
                return [`bench-sl-${w}-${i}`, 'H4sIAAAAAAAA', '131072', String(i % 131072), 'false', ts, 'caliper'];
            case 'RecordSignature':
                // (contractId, signerDid, signatureBase64, docHash, timestamp, updatedBy)
                return [`bench-contract-${w}-${i}`, `did:fabric:trustid:bench-${w}`, 'c2lnbmF0dXJl', DUMMY_HASH, ts, 'caliper'];
            case 'GetRecord':
            case 'GetRecordHistory':
                // (recordType, employeeId)
                return [this.seedRecordType || 'PROFILE', this.seedEmployeeId || `seed-w${w}`];
            case 'IsTrustedIssuer':
                return ['did:fabric:trustid:org1'];
            default:
                return [`bench-${w}-${i}`];
        }
    }

    async submitTransaction() {
        this.txIndex++;
        const request = {
            contractId: this.contractId,
            contractFunction: this.func,
            invokerIdentity: 'User1',
            contractArguments: this._genArgs(),
            readOnly: this.kind === 'evaluate',
        };
        await this.sutAdapter.sendRequests(request);
    }

    async cleanupWorkloadModule() {
        // không xoá dữ liệu để giữ audit trail.
    }
}

function createWorkloadModule() {
    return new IdentityWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
