package com.mpcorp.identity.infrastructures.persistence.jpa_entity

import com.mpcorp.identity.common.enums.AccountStatus
import com.mpcorp.identity.common.enums.EmployeeRole
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "auth")
data class AuthJpaEntity (
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(name = "email")
    var email: String,

    @Column(name = "phone")
    var phone: String,

    @Column(name = "password")
    var password: String?,

    @Column(name = "role")
    var role: EmployeeRole = EmployeeRole.EMPLOYEE,

    @Column(name = "account_status")
    @Enumerated(EnumType.STRING)
    var accountStatus: AccountStatus = AccountStatus.ACTIVE,
)