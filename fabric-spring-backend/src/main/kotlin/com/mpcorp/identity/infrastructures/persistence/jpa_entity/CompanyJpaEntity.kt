package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp
import java.time.LocalDate

@Entity
@Table(name = "company")
class CompanyJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "tax_code", unique = true, nullable = false)
    var taxCode: String,

    @Column(name = "company_name", nullable = false)
    var companyName: String,

    @Column(name = "legal_rep_name", nullable = false)
    var legalRepName: String,

    @Column(name = "legal_rep_title", nullable = false)
    var legalRepTitle: String,

    @Column(name = "legal_rep_id_number", nullable = false)
    var legalRepIdNumber: String,

    @Column(nullable = false)
    var address: String,

    @Column(nullable = false)
    var phone: String,

    @Column(nullable = false)
    var email: String,

    @Column(name = "registered_at", nullable = false)
    var registeredAt: LocalDate,

    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp,

    @Column(name = "created_by", nullable = false)
    var createdBy: String,
)
