package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "payroll")
data class PayrollJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "employee_id")
    var employee: EmployeeJpaEntity,

    @Column(name = "salary_type", nullable = false)
    var salaryType: String,

    @Column(name = "base_salary", nullable = false)
    var baseSalary: Double,

    @Column(name = "bonus_amount", nullable = true)
    var bonusSalary: Double?,

    @Column(name = "over_time_rate", nullable = true)
    var overTimeRate: Double?,

    @Column(name = "total_income", nullable = false)
    var totalIncome: Double,

    @Column(name = "currency", nullable = false)
    var currency: String,

    @Column(name = "payday", nullable = false)
    var payDay: Timestamp,

    @Column(name = "bank_account_number", nullable = false)
    var bankAccountNumber: String,

    @Column(name = "bank_account_name", nullable = false)
    var bankAccountName: String,

    @Column(name = "bank_name", nullable = false)
    var bankName: String,

    @Column(name = "bank_branch", nullable = true)
    var bankBranch: String?
)