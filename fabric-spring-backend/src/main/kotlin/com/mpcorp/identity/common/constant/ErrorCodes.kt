package com.mpcorp.identity.common.constant

object ErrorCodes {
    //Nhóm code thành công
    const val SUCCESS = 200
    const val CREATE_SUCCESS = 201
    const val DELETE_SUCCESS = 204
    const val UPDATE_SUCCESS = 205

    //Nhóm code lỗi client
    const val DATA_INCORRECT = 400
    const val UNAUTHORIZED = 401
    const val FORBIDDEN = 403
    const val NOT_FOUND = 404
    const val REQUEST_TIMEOUT = 408

    //Nhóm code lỗi server
    const val INTERNAL_SERVER_ERROR = 500
    const val SERVICE_UNAVAILABLE = 503
}