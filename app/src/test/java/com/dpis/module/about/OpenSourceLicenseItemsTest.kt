package com.dpis.module.about

import com.dpis.module.about.presentation.OpenSourceLicenseItems
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.entity.License
import com.mikepenz.aboutlibraries.entity.Organization
import com.mikepenz.aboutlibraries.entity.Scm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicenseItemsTest {
    private val labels = OpenSourceLicenseCatalogLabels(
        unknownName = "Unknown library",
        fallbackLicenseNames = "Unknown license",
        versionLabel = { version -> "Version: $version" },
        websiteLabel = { website -> "Website: $website" },
    )

    @Test
    fun fromLibraryPrefersWebsiteThenScmThenOrganizationThenLicenseUrl() {
        val withWebsite = OpenSourceLicenseItems.fromLibrary(
            library(
                website = "https://example.com",
                scmUrl = "https://scm.example",
                organizationUrl = "https://org.example",
                licenseUrl = "https://license.example",
            ),
            labels,
        )
        assertEquals("https://example.com", withWebsite.website)

        val fromScm = OpenSourceLicenseItems.fromLibrary(
            library(scmUrl = "https://scm.example", organizationUrl = "https://org.example"),
            labels,
        )
        assertEquals("https://scm.example", fromScm.website)

        val fromOrg = OpenSourceLicenseItems.fromLibrary(
            library(organizationUrl = "https://org.example"),
            labels,
        )
        assertEquals("https://org.example", fromOrg.website)

        val fromLicense = OpenSourceLicenseItems.fromLibrary(
            library(licenseUrl = "https://license.example"),
            labels,
        )
        assertEquals("https://license.example", fromLicense.website)
    }

    @Test
    fun fromLibraryBuildsSummaryAndDetailFromOfficialLibraryFields() {
        val item = OpenSourceLicenseItems.fromLibrary(
            library(
                name = "Example Lib",
                version = "1.2.3",
                website = "https://example.com",
                licenseName = "MIT",
                licenseUrl = "https://mit.example",
                licenseContent = "MIT TEXT",
            ),
            labels,
        )
        assertEquals("Example Lib", item.name)
        assertEquals("MIT", item.summary)
        assertTrue(item.detail.startsWith("Version: 1.2.3"))
        assertTrue(item.detail.contains("Website: https://example.com"))
        assertTrue(item.detail.contains("MIT\nhttps://mit.example\n\nMIT TEXT"))
    }

    @Test
    fun fromLibraryFallsBackWhenNameAndLicensesAreBlank() {
        val item = OpenSourceLicenseItems.fromLibrary(
            library(name = "  ", licenseName = "  "),
            labels,
        )
        assertEquals("Unknown library", item.name)
        assertEquals("Unknown license", item.summary)
        assertEquals("Unknown license", item.detail)
    }

    private fun library(
        name: String = "Example",
        version: String? = null,
        website: String? = null,
        scmUrl: String? = null,
        organizationUrl: String? = null,
        licenseName: String = "Apache-2.0",
        licenseUrl: String? = null,
        licenseContent: String? = null,
    ): Library {
        return Library(
            uniqueId = "com.example:lib",
            artifactVersion = version,
            name = name,
            description = null,
            website = website,
            developers = emptyList(),
            organization = organizationUrl?.let { Organization("Org", it) },
            scm = scmUrl?.let { Scm(connection = null, developerConnection = null, url = it) },
            licenses = setOf(
                License(
                    name = licenseName,
                    url = licenseUrl,
                    licenseContent = licenseContent,
                    hash = "hash",
                ),
            ),
        )
    }
}
