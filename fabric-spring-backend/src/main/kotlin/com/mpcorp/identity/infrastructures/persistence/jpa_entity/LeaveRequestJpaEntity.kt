package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp
import java.time.LocalDate

@Entity
@Table(name = "leave_request")
class LeaveRequestJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    var employee: EmployeeJpaEntity,

    // LEAVE | ATTENDANCE_CORRECTION | WFH | BUSINESS_TRIP
    @Column(name = "request_type", nullable = false)
    var requestType: String,

    // PENDING | APPROVED | REJECTED
    @Column(nullable = false)
    var status: String = "PENDING",

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    // FULL_DAY | MORNING | AFTERNOON (for LEAVE type)
    @Column
    var session: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var reason: String,

    @Column(name = "photo_url")
    var photoUrl: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    var approver: EmployeeJpaEntity? = null,

    @Column(name = "approved_at")
    var approvedAt: Timestamp? = null,

    @Column(name = "rejected_reason", columnDefinition = "TEXT")
    var rejectedReason: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp,
)
