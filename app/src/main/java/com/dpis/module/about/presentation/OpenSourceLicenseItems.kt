package com.dpis.module.about.presentation

import com.dpis.module.about.OpenSourceLicenseCatalog
import com.dpis.module.about.OpenSourceLicenseCatalogLabels
import com.dpis.module.about.OpenSourceLicenseItem

import android.content.Context
import android.content.res.Resources
import com.dpis.module.R
import java.io.InputStream
import java.nio.charset.StandardCharsets

object OpenSourceLicenseItems {
    fun load(context: Context): List<OpenSourceLicenseItem> {
        val project = createProjectLicenseItem(context)
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

    private fun emptyItem(context: Context, reason: String): OpenSourceLicenseItem {
        return OpenSourceLicenseItem(
            context.getString(R.string.open_source_license),
            reason,
            reason,
            "",
        )
    }

    private fun readText(inputStream: InputStream): String {
        return String(inputStream.readBytes(), StandardCharsets.UTF_8)
    }
}
