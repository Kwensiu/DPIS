package com.dpis.module.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.ui.compose.SupportActivityContent
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.charset.StandardCharsets

class OpenSourceLicenseActivity : LocalizedActivity() {
    class LicenseItem(
        val name: String,
        val summary: String,
        val detail: String,
        val website: String,
    )

    private class ResolvedLicense(
        val id: String,
        val name: String,
        val url: String,
        val content: String,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupportActivityContent.installOpenSourceLicenses(
            this,
            loadLicenseItems(),
            ::openUrl,
        )
    }

    private fun loadLicenseItems(): List<LicenseItem> {
        try {
            resources.openRawResource(R.raw.aboutlibraries).use { inputStream ->
                val rawJson = readText(inputStream)
                val root = JSONObject(rawJson)
                val libraries = root.optJSONArray("libraries")
                val licenseCatalog = root.optJSONObject("licenses")
                if (libraries == null || libraries.length() == 0) {
                    return listOf(
                        createProjectLicenseItem(),
                        emptyItem(getString(R.string.open_source_license_empty)),
                    )
                }

                val items = mutableListOf<LicenseItem>()
                items.add(createProjectLicenseItem())
                for (i in 0 until libraries.length()) {
                    val library = libraries.optJSONObject(i) ?: continue
                    val name = firstNonEmpty(
                        library.optString("name"),
                        library.optString("artifactId"),
                        library.optString("uniqueId"),
                        getString(R.string.open_source_license_name_unknown),
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
                        licenseNames = getString(R.string.open_source_license_item_fallback)
                    }
                    val website = firstNonEmpty(
                        library.optString("website"),
                        collectScmUrl(library),
                        collectOrganizationUrl(library),
                        collectFirstLicenseUrl(resolvedLicenses),
                    )

                    val detailBuilder = StringBuilder()
                    if (version.isNotEmpty()) {
                        detailBuilder.append(
                            getString(R.string.open_source_license_version_label, version),
                        )
                    }
                    if (website.isNotEmpty()) {
                        if (detailBuilder.isNotEmpty()) {
                            detailBuilder.append('\n')
                        }
                        detailBuilder.append(
                            getString(R.string.open_source_license_website_label, website),
                        )
                    }
                    val licenseDetail = buildLicenseDetail(resolvedLicenses)
                    if (detailBuilder.isNotEmpty()) {
                        detailBuilder.append('\n')
                    }
                    detailBuilder.append(
                        licenseDetail.ifEmpty { licenseNames },
                    )

                    items.add(LicenseItem(name, licenseNames, detailBuilder.toString(), website))
                }

                items.subList(1, items.size)
                    .sortWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
                return items
            }
        } catch (_: Resources.NotFoundException) {
            return listOf(
                createProjectLicenseItem(),
                emptyItem(getString(R.string.open_source_license_empty)),
            )
        } catch (_: Throwable) {
            return listOf(
                createProjectLicenseItem(),
                emptyItem(getString(R.string.open_source_license_load_failed)),
            )
        }
    }

    private fun createProjectLicenseItem(): LicenseItem {
        val sourceUrl = getString(R.string.about_source_url)
        val detailBuilder = StringBuilder(
            getString(R.string.open_source_license_project_detail, sourceUrl),
        )
        try {
            resources.openRawResource(R.raw.gpl_3_0).use { inputStream ->
                detailBuilder.append("\n\n").append(readText(inputStream))
            }
        } catch (_: Throwable) {
        }
        return LicenseItem(
            getString(R.string.app_name),
            getString(R.string.open_source_license_project_summary),
            detailBuilder.toString(),
            sourceUrl,
        )
    }

    private fun emptyItem(reason: String): LicenseItem {
        return LicenseItem(
            getString(R.string.open_source_license),
            reason,
            reason,
            "",
        )
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            if (isFinishing || isDestroyed) {
                return
            }
            Toast.makeText(this, R.string.about_link_open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
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

        private fun collectLicenseNames(resolvedLicenses: List<ResolvedLicense>?): String {
            if (resolvedLicenses.isNullOrEmpty()) {
                return ""
            }
            val names = resolvedLicenses.mapNotNull { license ->
                license.name.takeIf { it.isNotEmpty() }
            }
            return if (names.isEmpty()) "" else names.joinToString(", ")
        }

        private fun collectFirstLicenseUrl(resolvedLicenses: List<ResolvedLicense>?): String {
            if (resolvedLicenses.isNullOrEmpty()) {
                return ""
            }
            return resolvedLicenses.firstOrNull { it.url.isNotEmpty() }?.url.orEmpty()
        }

        private fun buildLicenseDetail(resolvedLicenses: List<ResolvedLicense>?): String {
            if (resolvedLicenses.isNullOrEmpty()) {
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

        private fun readText(inputStream: InputStream): String {
            return String(inputStream.readBytes(), StandardCharsets.UTF_8)
        }
    }
}
