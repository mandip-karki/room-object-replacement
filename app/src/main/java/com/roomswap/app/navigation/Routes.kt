package com.roomswap.app.navigation

object Routes {
    const val LOGIN = "login"

    const val SUPER_ADMIN_COMPANIES = "super_admin/companies"
    const val SUPER_ADMIN_CATALOG = "super_admin/catalog"

    const val CLIENT_ADMIN_SUB_ACCOUNTS = "client_admin/sub_accounts"
    const val CLIENT_ADMIN_CATALOG = "client_admin/catalog"

    const val ROOM_PHOTO = "sub_account/room_photo"
    const val PRODUCT_PICKER = "sub_account/product_picker"
    const val RESULT = "sub_account/result/{jobId}"

    fun result(jobId: String) = "sub_account/result/$jobId"
}
