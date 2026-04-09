package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "contract")
data class ContractJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "employee_id")
    var employee: EmployeeJpaEntity,

    @Column(name = "type_contract", nullable = false)
    var typeContract: String,

    @Column(name = "start_date", nullable = false)
    var startDate: Timestamp,

    @Column(name = "end_date", nullable = true)
    var endDate: Timestamp?,

    @Column(name = "contract_expire", nullable = true)
    var contractExpire: Timestamp?,

    @Column(name = "probation_start_date", nullable = true)
    var probationStartDate: Timestamp?,

    @Column(name = "probation_end_date", nullable = true)
    var probationEndDate: Timestamp?,

    @Column(name = "tax_code", nullable = false, unique = true)
    var taxCode: String,

    @Column(name = "social_insurance_number", nullable = true, unique = true)
    var socialInsuranceNumber: String?,

    @Column(name = "health_insurance_number", nullable = true, unique = true)
    var healthInsuranceNumber: String?,

    )