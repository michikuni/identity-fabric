package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*

@Entity
@Table(name = "profile")
data class ProfileJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @OneToOne
    @JoinColumn(name = "employee_id")
    var employee: EmployeeJpaEntity,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "gender", nullable = false)
    var gender: String,

    @Column(name = "identity_type", nullable = false)
    var identityType: String,

    @Column(name = "identity_number", nullable = false, unique = true)
    var identityNumber: String,

    @Column(name = "identity_issue_date", nullable = false)
    var identityIssueDate: Int,

    @Column(name = "identity_issue_place", nullable = false)
    var identityIssuePlace: String,

    @Column(nullable = false)
    var email: String,

    @Column(nullable = false)
    var phone: String,

    @Column(name = "emergency_name", nullable = false)
    var emergencyName: String,

    @Column(name = "emergency_phone", nullable = false)
    var emergencyPhone: String,

    @Column(name = "emergency_relationship", nullable = false)
    var emergencyRelationship: String,

    @Column(name = "date_of_birth", nullable = false)
    var dateOfBirth: String,

    @Column(nullable = false)
    var health: String,

    @Column(nullable = false)
    var married: String,

    @Column(name = "permanent_residence", nullable = false)
    var permanentResidence: String,

    @Column(name = "now_residence", nullable = false)
    var nowResidence: String,

    @Column(name = "avatar_url")
    var avatarUrl: String?,

    @Column(name = "education_level", nullable = false)
    var educationLevel: String,

    @Column(nullable = false)
    var major: String,

    @ElementCollection
    @CollectionTable(name = "employee_certificates", joinColumns = [JoinColumn(name = "employee_id")])
    @Column(name = "certificate")
    var certificate: List<String>? = null,

    @ElementCollection
    @CollectionTable(name = "employee_skills", joinColumns = [JoinColumn(name = "employee_id")])
    @Column(name = "skill")
    var skillSet: List<String> = listOf(),

    @Column(name = "exp_years", nullable = false)
    var expYears: Int
)