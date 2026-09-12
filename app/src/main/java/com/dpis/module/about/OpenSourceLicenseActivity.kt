package com.dpis.module.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import com.dpis.module.settings.LocalizedActivity
import com.dpis.module.R
import com.dpis.module.about.presentation.SupportActivityContent
import java.io.InputStream
import java.nio.charset.StandardCharsets

class OpenSourceLicenseActivity : LocalizedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupportActivityContent.installOpenSourceLicenses(
            this,
            loadLicenseItems(),
            ::openUrl,
        )
    }

    private fun loadLicenseItems(): List<OpenSourceLicenseItem> {
        val project = createProjectLicenseItem()
        return try {
            resources.openRawResource(R.raw.aboutlibraries).use { inputStream ->
                val libraries = OpenSourceLicenseCatalog.parseLibraryItems(
                    readText(inputStream),
                    catalogLabels(),
                )
                if (libraries.isEmpty()) {
                    listOf(
                        project,
                        emptyItem(getString(R.string.open_source_license_empty)),
                    )
                } else {
                    listOf(project) + libraries
                }
            }
        } catch (_: Resources.NotFoundException) {
            listOf(
                project,
                emptyItem(getString(R.string.open_source_license_empty)),
            )
        } catch (_: Throwable) {
            listOf(
                project,
                emptyItem(getString(R.string.open_source_license_load_failed)),
            )
        }
    }

    private fun catalogLabels(): OpenSourceLicenseCatalogLabels {
        return OpenSourceLicenseCatalogLabels(
            unknownName = getString(R.string.open_source_license_name_unknown),
            fallbackLicenseNames = getString(R.string.open_source_license_item_fallback),
            versionLabel = { version ->
                getString(R.string.open_source_license_version_label, version)
            },
            websiteLabel = { website ->
                getString(R.string.open_source_license_website_label, website)
            },
        )
    }

    private fun createProjectLicenseItem(): OpenSourceLicenseItem {
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
        return OpenSourceLicenseItem(
            getString(R.string.app_name),
            getString(R.string.open_source_license_project_summary),
            detailBuilder.toString(),
            sourceUrl,
        )
    }

    private fun emptyItem(reason: String): OpenSourceLicenseItem {
        return OpenSourceLicenseItem(
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

    private fun readText(inputStream: InputStream): String {
        return String(inputStream.readBytes(), StandardCharsets.UTF_8)
    }
}
