package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import jakarta.persistence.*
import java.sql.Timestamp
import java.time.LocalDate

@Entity
@Table(name = "attendance", uniqueConstraints = [UniqueConstraint(columnNames = ["employee_id", "work_date"])])
class AttendanceJpaEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    var employee: EmployeeJpaEntity,

    @Column(name = "work_date", nullable = false)
    var workDate: LocalDate,

    @Column(name = "check_in_time")
    var checkInTime: Timestamp? = null,

    @Column(name = "check_out_time")
    var checkOutTime: Timestamp? = null,

    @Column(name = "check_in_location")
    var checkInLocation: String? = null,

    @Column(name = "check_out_location")
    var checkOutLocation: String? = null,

    // PRESENT | LATE | HALF_DAY | ABSENT
    @Column(nullable = false)
    var status: String = "PRESENT",

    @Column(columnDefinition = "TEXT")
    var note: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Timestamp,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Timestamp,
)
