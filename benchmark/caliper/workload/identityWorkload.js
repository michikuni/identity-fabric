'use strict';

// identityWorkload.js — Workload module dùng chung cho mọi giao dịch chaincode TrustID.
//
// Mỗi vòng trong benchmarkConfig.yaml truyền vào:
//   - contractId: tên chaincode (identity-ledger)
//   - func:       tên hàm chaincode (RegisterDID, UpsertRecord, GetRecord, ...)
//   - kind:       'submit' (ghi) hoặc 'evaluate' (đọc)
//   - keyPrefix:  tiền tố key để sinh dữ liệu không trùng (vd 'did', 'profile', 'statuslist')
//
// Workload sinh tham số HỢP LỆ và gửi giao dịch THẬT lên Fabric. Caliper đo TPS/latency
// thật của các giao dịch này; không có số liệu nào được bịa ra.

const { WorkloadModuleBase } = require('@hyperledger/caliper-core');

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

        // Với các giao dịch ĐỌC (evaluate), cần dữ liệu tồn tại trước. Nạp sẵn một số bản ghi.
        if (this.kind === 'evaluate') {
            const seedKey = `${this.keyPrefix}:seed-w${this.workerIndex}`;
            try {
                await this.sutAdapter.sendRequests({
                    contractId: this.contractId,
                    contractFunction: 'UpsertRecord',
                    invokerIdentity: 'User1',
                    contractArguments: [seedKey, JSON.stringify({ seeded: true, ts: Date.now() })],
                    readOnly: false,
                });
                this.seedKey = seedKey;
            } catch (e) {
                // nếu chaincode dùng tên hàm khác cho ghi, chỉnh ở đây
                this.seedKey = seedKey;
            }
        }
    }

    _genArgs() {
        const uid = `${this.keyPrefix}:w${this.workerIndex}-${this.txIndex}-${Date.now()}`;
        switch (this.func) {
            case 'RegisterDID':
                return [`did:fabric:trustid:bench-${this.workerIndex}-${this.txIndex}`,
                        JSON.stringify({ '@context': 'https://www.w3.org/ns/did/v1', id: `did:fabric:trustid:bench-${this.txIndex}` })];
            case 'UpsertRecord':
            case 'CreateProfile':
                return [uid, JSON.stringify({ employeeId: this.txIndex, hash: `h${this.txIndex}`, ts: Date.now() })];
            case 'UpdateStatusListEntry':
                return ['employment-status-list-1', String(this.txIndex % 131072), '1'];
            case 'RecordSignature':
                return [`contract:bench-${this.txIndex}`, `did:fabric:trustid:bench-${this.workerIndex}`, `sig-${this.txIndex}`];
            case 'GetRecord':
                return [this.seedKey || `${this.keyPrefix}:w${this.workerIndex}`];
            case 'GetRecordHistory':
                return [this.seedKey || `${this.keyPrefix}:w${this.workerIndex}`];
            case 'IsTrustedIssuer':
                return ['did:fabric:trustid:org1'];
            default:
                return [uid];
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
        // không xoá dữ liệu để giữ audit trail; soft-delete nếu cần.
    }
}

function createWorkloadModule() {
    return new IdentityWorkload();
}

module.exports.createWorkloadModule = createWorkloadModule;
