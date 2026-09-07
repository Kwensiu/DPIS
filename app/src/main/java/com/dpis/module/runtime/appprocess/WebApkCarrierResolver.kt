package com.dpis.module.runtime.appprocess

object WebApkCarrierResolver {
    const val WEBAPK_PACKAGE_PREFIX: String = "org.chromium.webapk."
    const val WEBAPK_PACKAGE_EXTRA: String = "org.chromium.chrome.browser.webapk_package_name"
    private const val WEBAPK_URI_PREFIX = "webapp://webapk-"

    @JvmStatic
    fun isWebApkOwnerPackage(packageName: String?): Boolean {
        return isLikelyPackageName(packageName)
                && packageName!!.startsWith(WEBAPK_PACKAGE_PREFIX)
    }

    @JvmStatic
    fun collectOwnerPackagesFromText(text: String?, maxCount: Int): MutableSet<String?> {
        val owners: MutableSet<String?> = LinkedHashSet<String?>()
        collectByPrefix(text, WEBAPK_PACKAGE_PREFIX, owners, maxCount)
        collectFromWebAppUri(text, owners, maxCount)
        return owners
    }

    @JvmStatic
    fun ownerPackageFromText(text: String?): String? {
        return collectOwnerPackagesFromText(text, 1).stream().findFirst().orElse(null)
    }

    private fun collectFromWebAppUri(text: String?, output: MutableSet<String?>?, maxCount: Int) {
        if (text == null || output == null || output.size >= maxCount) {
            return
        }
        var index = text.indexOf(WEBAPK_URI_PREFIX)
        while (index >= 0 && output.size < maxCount) {
            val start = index + WEBAPK_URI_PREFIX.length
            val candidate = readPackageToken(text, start)
            if (isWebApkOwnerPackage(candidate)) {
                output.add(candidate)
            }
            index = text.indexOf(WEBAPK_URI_PREFIX, start)
        }
    }

    private fun collectByPrefix(
        text: String?,
        prefix: String?,
        output: MutableSet<String?>?,
        maxCount: Int
    ) {
        if (text == null || prefix == null || output == null || maxCount <= 0) {
            return
        }
        var index = text.indexOf(prefix)
        while (index >= 0 && output.size < maxCount) {
            val candidate = readPackageToken(text, index)
            if (isWebApkOwnerPackage(candidate)) {
                output.add(candidate)
            }
            index = text.indexOf(prefix, index + prefix.length)
        }
    }

    private fun readPackageToken(text: String?, start: Int): String? {
        if (text == null || start < 0 || start >= text.length) {
            return null
        }
        var end = start
        while (end < text.length) {
            val c = text.get(end)
            if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                end++
                continue
            }
            break
        }
        return if (end > start) text.substring(start, end) else null
    }

    private fun isLikelyPackageName(value: String?): Boolean {
        if (value == null || value.isBlank() || value.length > 256 || !value.contains(".")) {
            return false
        }
        for (i in 0..<value.length) {
            val c = value.get(i)
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '.')) {
                return false
            }
        }
        return Character.isLowerCase(value.get(0))
    }
}
