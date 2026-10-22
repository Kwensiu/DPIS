package com.dpis.module.config

internal const val PACKAGE_IDENTITY_MIGRATION_OWNER_USER_ID =
    "package_identity.migration_v1.owner_user_id"

data class PackageIdentity(
    val packageName: String,
    val userId: Int,
) {
    init {
        require(isValidPackageName(packageName)) { "packageName is invalid" }
        require(userId >= 0) { "userId must not be negative" }
    }

    fun packageConfigKey(field: String): String {
        require(CONFIG_FIELD_PATTERN.matches(field)) { "field is invalid" }
        return "$PACKAGE_CONFIG_PREFIX$userId.$packageName.$field"
    }

    fun packageConfigResidualKey(field: String): String {
        require(CONFIG_FIELD_PATTERN.matches(field)) { "field is invalid" }
        return "$PACKAGE_CONFIG_RESIDUAL_PREFIX$userId.$packageName.$field"
    }

    companion object {
        private const val PACKAGE_CONFIG_PREFIX = "package_identity."
        private const val PACKAGE_CONFIG_RESIDUAL_PREFIX = "package_identity_residual."
        private val PACKAGE_NAME_PATTERN =
            Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)*")
        private val CONFIG_FIELD_PATTERN = Regex("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")

        fun isValidPackageName(packageName: String): Boolean =
            packageName.length <= 255 && PACKAGE_NAME_PATTERN.matches(packageName)
    }
}
