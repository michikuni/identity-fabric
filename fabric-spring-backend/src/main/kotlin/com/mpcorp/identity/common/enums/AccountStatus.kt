package com.mpcorp.identity.common.enums

enum class AccountStatus {
    PENDING,   // Đăng ký xong, chờ Admin duyệt
    ACTIVE,    // Đã được duyệt
    REJECTED,  // Bị từ chối
}
