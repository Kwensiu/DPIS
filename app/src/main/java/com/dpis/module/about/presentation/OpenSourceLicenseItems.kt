package com.dpis.module.about.presentation

import android.content.Context
import android.content.res.Resources
import com.dpis.module.R
import com.dpis.module.about.OpenSourceLicenseCatalog
import com.dpis.module.about.OpenSourceLicenseCatalogLabels
import com.dpis.module.about.OpenSourceLicenseItem
import com.mikepenz.aboutlibraries.entity.Library
import java.io.InputStream
import java.nio.charset.StandardCharsets

object OpenSourceLicenseItems {
    fun projectItem(context: Context): OpenSourceLicenseItem = createProjectLicenseItem(context)

    fun emptyItem(context: Context, reason: String): OpenSourceLicenseItem {
        return OpenSourceLicenseItem(
            context.getString(R.string.open_source_license),
            reason,
            reason,
            "",
        )
    }

    fun fromLibrary(library: Library, context: Context): OpenSourceLicenseItem {
        return fromLibrary(library, catalogLabels(context))
    }

    internal fun fromLibrary(
        library: Library,
        labels: OpenSourceLicenseCatalogLabels,
    ): OpenSourceLicenseItem {
        val licenseNames = library.licenses
            .map { it.name.trim() }
            .filter { it.isNotEmpty() }
            .joinToString()
            .ifEmpty { labels.fallbackLicenseNames }
        val website = firstNonEmpty(
            library.website,
            library.scm?.url,
            library.organization?.url,
            library.licenses.firstOrNull { !it.url.isNullOrBlank() }?.url,
        )
        val detail = buildString {
            val version = library.artifactVersion?.trim().orEmpty()
            if (version.isNotEmpty()) {
                append(labels.versionLabel(version))
            }
            if (website.isNotEmpty()) {
                if (isNotEmpty()) {
                    append('\n')
                }
                append(labels.websiteLabel(website))
            }
            library.licenses.forEach { license ->
                val licenseName = license.name.trim()
                val licenseUrl = license.url?.trim().orEmpty()
                val content = license.licenseContent?.trim().orEmpty()
                if (licenseName.isEmpty() && licenseUrl.isEmpty() && content.isEmpty()) {
                    return@forEach
                }
                if (isNotEmpty()) {
                    append("\n\n")
                }
                if (licenseName.isNotEmpty()) {
                    append(licenseName)
                }
                if (licenseUrl.isNotEmpty()) {
                    if (isNotEmpty() && licenseName.isNotEmpty()) {
                        append('\n')
                    }
                    append(licenseUrl)
                }
                if (content.isNotEmpty()) {
                    if (isNotEmpty()) {
                        append("\n\n")
                    }
                    append(content)
                }
            }
            if (isEmpty()) {
                append(licenseNames)
            }
        }
        return OpenSourceLicenseItem(
            library.name.trim().ifEmpty { labels.unknownName },
            licenseNames,
            detail,
            website,
        )
    }

    fun load(context: Context): List<OpenSourceLicenseItem> {
        val project = projectItem(context)
        return try {
            context.resources.openRawResource(R.raw.aboutlibraries).use { inputStream ->
                val libraries = OpenSourceLicenseCatalog.parseLibraryItems(
                    readText(inputStream),
                    catalogLabels(context),
                )
                if (libraries.isEmpty()) {
                    listOf(
                        project,
                        emptyItem(context, context.getString(R.string.open_source_license_empty)),
                    )
                } else {
                    listOf(project) + libraries
                }
            }
        } catch (_: Resources.NotFoundException) {
            listOf(
                project,
                emptyItem(context, context.getString(R.string.open_source_license_empty)),
            )
        } catch (_: Throwable) {
            listOf(
                project,
                emptyItem(context, context.getString(R.string.open_source_license_load_failed)),
            )
        }
    }

    private fun catalogLabels(context: Context): OpenSourceLicenseCatalogLabels {
        return OpenSourceLicenseCatalogLabels(
            unknownName = context.getString(R.string.open_source_license_name_unknown),
            fallbackLicenseNames = context.getString(R.string.open_source_license_item_fallback),
            versionLabel = { version ->
                context.getString(R.string.open_source_license_version_label, version)
            },
            websiteLabel = { website ->
                context.getString(R.string.open_source_license_website_label, website)
            },
        )
    }

    private fun createProjectLicenseItem(context: Context): OpenSourceLicenseItem {
        val sourceUrl = context.getString(R.string.about_source_url)
        val detailBuilder = StringBuilder(
            context.getString(R.string.open_source_license_project_detail, sourceUrl),
        )
        try {
            context.resources.openRawResource(R.raw.gpl_3_0).use { inputStream ->
                detailBuilder.append("\n\n").append(readText(inputStream))
            }
        } catch (_: Throwable) {
        }
        return OpenSourceLicenseItem(
            context.getString(R.string.app_name),
            context.getString(R.string.open_source_license_project_summary),
            detailBuilder.toString(),
            sourceUrl,
        )
    }

    private fun readText(inputStream: InputStream): String {
        return String(inputStream.readBytes(), StandardCharsets.UTF_8)
    }

    private fun firstNonEmpty(vararg values: String?): String {
        for (value in values) {
            val trimmed = value?.trim().orEmpty()
            if (trimmed.isNotEmpty()) {
                return trimmed
            }
        }
        return ""
    }
}
