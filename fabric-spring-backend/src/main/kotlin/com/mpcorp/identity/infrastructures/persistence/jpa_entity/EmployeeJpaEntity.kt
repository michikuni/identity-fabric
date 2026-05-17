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
    var payroll: PayrollJpaEntity? = null,

    // DID Layer — issued when Admin approves the account
    @Column(name = "did", unique = true)
    var did: String? = null,

    @Column(name = "public_key", columnDefinition = "TEXT")
    var publicKey: String? = null,

    // VC Layer — EmploymentVC JSON issued on approve
    @Column(name = "employment_vc", columnDefinition = "LONGTEXT")
    var employmentVc: String? = null,

    // VC Layer — TerminationVC JSON issued on terminate
    @Column(name = "termination_vc", columnDefinition = "LONGTEXT")
    var terminationVc: String? = null,

    // VC Layer — SalaryRangeVC JSON issued when payroll is assigned
    @Column(name = "salary_range_vc", columnDefinition = "LONGTEXT")
    var salaryRangeVc: String? = null,

    // VC Layer — PromotionVC JSON issued when role/position changes
    @Column(name = "promotion_vc", columnDefinition = "LONGTEXT")
    var promotionVc: String? = null,

    // SD-JWT Layer — SkillCredential as SD-JWT compact string (selective disclosure)
    @Column(name = "skill_sd_jwt", columnDefinition = "LONGTEXT")
    var skillSdJwt: String? = null,

    // SD-JWT Layer — EducationCredential as SD-JWT compact string
    @Column(name = "education_sd_jwt", columnDefinition = "LONGTEXT")
    var educationSdJwt: String? = null,

    // VC Layer — TrainingVC JSON issued when employee completes training
    @Column(name = "training_vc", columnDefinition = "LONGTEXT")
    var trainingVc: String? = null,

    // VC Layer — NDA-AcceptedVC JSON issued when employee signs NDA
    @Column(name = "nda_accepted_vc", columnDefinition = "LONGTEXT")
    var ndaAcceptedVc: String? = null,
)