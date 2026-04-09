package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp

@Entity
@Table(name = "employee")
class EmployeeJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "auth_id", nullable = false)
    var auth: AuthJpaEntity,

    @Column(nullable = false)
    var department: String,

    @Column(nullable = false)
    var position: String,

    @Column(nullable = false)
    var status: String,

    @Column(name = "working_type", nullable = false)
    var workingType: String,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean,

    @ManyToOne
    @JoinColumn(name = "manager_id")
    var manager: EmployeeJpaEntity?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp,

    @Column(name = "created_by", nullable = false)
    var createdBy: String,

    @Column(columnDefinition = "TEXT")
    var note: String?,

    @OneToOne(mappedBy = "employee", cascade = [CascadeType.ALL])
    var profile: ProfileJpaEntity? = null,

    @OneToOne(mappedBy = "employee", cascade = [CascadeType.ALL])
    var contract: ContractJpaEntity? = null,

    @OneToOne(mappedBy = "employee", cascade = [CascadeType.ALL])
    var payroll: PayrollJpaEntity? = null
)