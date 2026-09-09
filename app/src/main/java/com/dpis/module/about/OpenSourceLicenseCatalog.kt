package com.dpis.module.about

import org.json.JSONArray
import org.json.JSONObject

class OpenSourceLicenseItem(
    val name: String,
    val summary: String,
    val detail: String,
    val website: String,
)

internal class OpenSourceLicenseCatalogLabels(
    val unknownName: String,
    val fallbackLicenseNames: String,
    val versionLabel: (String) -> String,
    val websiteLabel: (String) -> String,
)

/** Parses AboutLibraries JSON into display items without Android resource access. */
internal object OpenSourceLicenseCatalog {
    fun parseLibraryItems(
        rawJson: String,
        labels: OpenSourceLicenseCatalogLabels,
    ): List<OpenSourceLicenseItem> {
        val root = JSONObject(rawJson)
        val libraries = root.optJSONArray("libraries")
        val licenseCatalog = root.optJSONObject("licenses")
        if (libraries == null || libraries.length() == 0) {
            return emptyList()
        }

        val items = mutableListOf<OpenSourceLicenseItem>()
        for (i in 0 until libraries.length()) {
            val library = libraries.optJSONObject(i) ?: continue
            items.add(parseLibrary(library, licenseCatalog, labels))
        }
        items.sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        return items
    }

    private fun parseLibrary(
        library: JSONObject,
        licenseCatalog: JSONObject?,
        labels: OpenSourceLicenseCatalogLabels,
    ): OpenSourceLicenseItem {
        val name = firstNonEmpty(
            library.optString("name"),
            library.optString("artifactId"),
            library.optString("uniqueId"),
            labels.unknownName,
        )
        val version = firstNonEmpty(
            library.optString("artifactVersion"),
            library.optString("version"),
            "",
        )
        val resolvedLicenses = resolveLicenses(
            library.optJSONArray("licenses"),
            licenseCatalog,
        )
        var licenseNames = collectLicenseNames(resolvedLicenses)
        if (licenseNames.isEmpty()) {
            licenseNames = labels.fallbackLicenseNames
        }
        val website = firstNonEmpty(
            library.optString("website"),
            collectScmUrl(library),
            collectOrganizationUrl(library),
            collectFirstLicenseUrl(resolvedLicenses),
        )

        val detailBuilder = StringBuilder()
        if (version.isNotEmpty()) {
            detailBuilder.append(labels.versionLabel(version))
        }
        if (website.isNotEmpty()) {
            if (detailBuilder.isNotEmpty()) {
                detailBuilder.append('\n')
            }
            detailBuilder.append(labels.websiteLabel(website))
        }
        val licenseDetail = buildLicenseDetail(resolvedLicenses)
        if (detailBuilder.isNotEmpty()) {
            detailBuilder.append('\n')
        }
        detailBuilder.append(licenseDetail.ifEmpty { licenseNames })
        return OpenSourceLicenseItem(name, licenseNames, detailBuilder.toString(), website)
    }

    private fun resolveLicenses(
        licenses: JSONArray?,
        licenseCatalog: JSONObject?,
    ): List<ResolvedLicense> {
        if (licenses == null || licenses.length() == 0) {
            return emptyList()
        }

        val resolvedLicenses = mutableListOf<ResolvedLicense>()
        val seenIds = linkedSetOf<String>()
        for (i in 0 until licenses.length()) {
            val resolved = resolveLicenseEntry(licenses.opt(i), licenseCatalog) ?: continue
            val dedupeKey = firstNonEmpty(
                resolved.id,
                resolved.name,
                resolved.url,
                i.toString(),
            )
            if (!seenIds.add(dedupeKey)) {
                continue
            }
            resolvedLicenses.add(resolved)
        }
        return resolvedLicenses
    }

    private fun collectLicenseNames(resolvedLicenses: List<ResolvedLicense>): String {
        val names = resolvedLicenses.mapNotNull { license ->
            license.name.takeIf { it.isNotEmpty() }
        }
        return if (names.isEmpty()) "" else names.joinToString(", ")
    }

    private fun collectFirstLicenseUrl(resolvedLicenses: List<ResolvedLicense>): String {
        return resolvedLicenses.firstOrNull { it.url.isNotEmpty() }?.url.orEmpty()
    }

    private fun buildLicenseDetail(resolvedLicenses: List<ResolvedLicense>): String {
        if (resolvedLicenses.isEmpty()) {
            return ""
        }
        val detailBuilder = StringBuilder()
        for (license in resolvedLicenses) {
            if (detailBuilder.isNotEmpty()) {
                detailBuilder.append("\n\n")
            }
            detailBuilder.append(license.name)
            if (license.url.isNotEmpty()) {
                detailBuilder.append('\n').append(license.url)
            }
            if (license.content.isNotEmpty()) {
                detailBuilder.append("\n\n").append(license.content)
            }
        }
        return detailBuilder.toString()
    }

    private fun resolveLicenseEntry(
        entry: Any?,
        licenseCatalog: JSONObject?,
    ): ResolvedLicense? {
        if (entry is String) {
            return mergeLicenseInfo(entry, null, licenseCatalog)
        }
        if (entry is JSONObject) {
            val key = firstNonEmpty(
                entry.optString("internalHash"),
                entry.optString("spdxId"),
                entry.optString("hash"),
                "",
            )
            return mergeLicenseInfo(key, entry, licenseCatalog)
        }
        return null
    }

    private fun mergeLicenseInfo(
        key: String?,
        inline: JSONObject?,
        licenseCatalog: JSONObject?,
    ): ResolvedLicense? {
        val normalizedKey = firstNonEmpty(key, "")
        val catalog = if (normalizedKey.isEmpty() || licenseCatalog == null) {
            null
        } else {
            licenseCatalog.optJSONObject(normalizedKey)
        }
        val id = firstNonEmpty(
            inline?.optString("internalHash").orEmpty(),
            inline?.optString("spdxId").orEmpty(),
            inline?.optString("hash").orEmpty(),
            catalog?.optString("internalHash").orEmpty(),
            catalog?.optString("spdxId").orEmpty(),
            normalizedKey,
        )
        val name = firstNonEmpty(
            inline?.optString("name").orEmpty(),
            inline?.optString("spdxId").orEmpty(),
            catalog?.optString("name").orEmpty(),
            catalog?.optString("spdxId").orEmpty(),
            id,
        )
        val url = firstNonEmpty(
            inline?.optString("url").orEmpty(),
            catalog?.optString("url").orEmpty(),
            "",
        )
        val content = firstNonEmpty(
            inline?.optString("content").orEmpty(),
            catalog?.optString("content").orEmpty(),
            "",
        )

        if (name.isEmpty() && url.isEmpty() && content.isEmpty()) {
            return null
        }
        return ResolvedLicense(id, name, url, content)
    }

    private fun collectScmUrl(library: JSONObject): String {
        when (val scm = library.opt("scm")) {
            is JSONObject -> {
                return firstNonEmpty(
                    scm.optString("url"),
                    scm.optString("developerConnection"),
                    scm.optString("connection"),
                    "",
                )
            }
            is String -> return firstNonEmpty(scm, "")
        }
        return ""
    }

    private fun collectOrganizationUrl(library: JSONObject): String {
        val direct = firstNonEmpty(library.optString("organizationUrl"), "")
        if (direct.isNotEmpty()) {
            return direct
        }
        val organization = library.opt("organization")
        if (organization is JSONObject) {
            return firstNonEmpty(
                organization.optString("url"),
                organization.optString("organisationUrl"),
                "",
            )
        }
        return ""
    }

    private fun firstNonEmpty(vararg values: String?): String {
        for (value in values) {
            if (value != null) {
                val trimmed = value.trim()
                if (trimmed.isNotEmpty()) {
                    return trimmed
                }
            }
        }
        return ""
    }

    private class ResolvedLicense(
        val id: String,
        val name: String,
        val url: String,
        val content: String,
    )
}
