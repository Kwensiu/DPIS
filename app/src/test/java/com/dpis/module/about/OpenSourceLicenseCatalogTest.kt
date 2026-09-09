package com.dpis.module.about

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenSourceLicenseCatalogTest {
    private val labels = OpenSourceLicenseCatalogLabels(
        unknownName = "Unknown library",
        fallbackLicenseNames = "Unknown license",
        versionLabel = { "Version: $it" },
        websiteLabel = { "Website: $it" },
    )

    @Test
    fun emptyLibrariesArrayReturnsNoItems() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """{"libraries":[],"licenses":{}}""",
            labels,
        )

        assertTrue(items.isEmpty())
    }

    @Test
    fun missingLibrariesKeyReturnsNoItems() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems("{}", labels)

        assertTrue(items.isEmpty())
    }

    @Test
    fun stringLicenseReferenceResolvesNameUrlAndContentFromCatalog() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "licenses": {
                "Apache-2.0": {
                  "name": "Apache License 2.0",
                  "url": "https://www.apache.org/licenses/LICENSE-2.0",
                  "content": "Apache text"
                }
              },
              "libraries": [
                {
                  "name": "Guava",
                  "artifactVersion": "33.0",
                  "website": "https://guava.dev",
                  "licenses": ["Apache-2.0"]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Guava", item.name)
        assertEquals("Apache License 2.0", item.summary)
        assertEquals("https://guava.dev", item.website)
        assertEquals(
            "Version: 33.0\nWebsite: https://guava.dev\nApache License 2.0\nhttps://www.apache.org/licenses/LICENSE-2.0\n\nApache text",
            item.detail,
        )
    }

    @Test
    fun inlineLicenseObjectMergesWithCatalogAndKeepsInlineFields() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "licenses": {
                "MIT": {
                  "name": "Catalog MIT",
                  "url": "https://catalog.mit",
                  "content": "Catalog text"
                }
              },
              "libraries": [
                {
                  "name": "InlineLib",
                  "licenses": [
                    {
                      "spdxId": "MIT",
                      "name": "Inline MIT",
                      "url": "https://inline.mit",
                      "content": "Inline text"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        val item = items.single()
        assertEquals("Inline MIT", item.summary)
        assertEquals("https://inline.mit", item.website)
        assertEquals(
            "Website: https://inline.mit\nInline MIT\nhttps://inline.mit\n\nInline text",
            item.detail,
        )
    }

    @Test
    fun nameFallsBackThroughArtifactIdUniqueIdAndUnknownLabel() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {"artifactId": "from-artifact", "licenses": []},
                {"uniqueId": "from-unique", "licenses": []},
                {"licenses": []}
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals(
            listOf("from-artifact", "from-unique", "Unknown library"),
            items.map { it.name },
        )
        assertTrue(items.all { it.summary == "Unknown license" })
    }

    @Test
    fun websiteFallsBackThroughScmOrganizationAndLicenseUrl() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {
                  "name": "scm-object",
                  "scm": {"url": "https://scm.example/object"},
                  "licenses": []
                },
                {
                  "name": "scm-string",
                  "scm": "https://scm.example/string",
                  "licenses": []
                },
                {
                  "name": "org-url",
                  "organizationUrl": "https://org.example/direct",
                  "licenses": []
                },
                {
                  "name": "org-object",
                  "organization": {"url": "https://org.example/nested"},
                  "licenses": []
                },
                {
                  "name": "license-url",
                  "licenses": [{"name": "MIT", "url": "https://mit.example"}]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals(
            mapOf(
                "license-url" to "https://mit.example",
                "org-object" to "https://org.example/nested",
                "org-url" to "https://org.example/direct",
                "scm-object" to "https://scm.example/object",
                "scm-string" to "https://scm.example/string",
            ),
            items.associate { it.name to it.website },
        )
    }

    @Test
    fun sortsLibraryNamesCaseInsensitivelyAndSkipsNonObjects() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {"name": "zeta", "licenses": []},
                null,
                "skip-me",
                {"name": "Alpha", "licenses": []}
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals(listOf("Alpha", "zeta"), items.map { it.name })
    }

    @Test
    fun duplicateLicenseKeysAreKeptOnce() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "licenses": {
                "MIT": {"name": "MIT License", "url": "https://mit.example"}
              },
              "libraries": [
                {
                  "name": "Duped",
                  "licenses": ["MIT", "MIT"]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals("MIT License", items.single().summary)
        assertEquals("https://mit.example", items.single().website)
        assertEquals(
            "Website: https://mit.example\nMIT License\nhttps://mit.example",
            items.single().detail,
        )
    }

    @Test
    fun blankLicenseEntriesAreIgnored() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {
                  "name": "Blank",
                  "licenses": ["", {}]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals("Unknown license", items.single().summary)
        assertEquals("Unknown license", items.single().detail)
    }

    @Test
    fun versionPrefersArtifactVersionOverVersion() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {
                  "name": "Versioned",
                  "artifactVersion": "2.0",
                  "version": "1.0",
                  "licenses": []
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertTrue(items.single().detail.startsWith("Version: 2.0\n"))
    }

    @Test
    fun scmPrefersUrlThenDeveloperConnectionThenConnection() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {
                  "name": "scm-fallback",
                  "scm": {
                    "developerConnection": "https://dev.example",
                    "connection": "https://conn.example"
                  },
                  "licenses": []
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals("https://dev.example", items.single().website)
    }

    @Test
    fun invalidJsonPropagatesParseFailure() {
        assertThrows(JSONException::class.java) {
            OpenSourceLicenseCatalog.parseLibraryItems("{", labels)
        }
    }

    @Test
    fun joinsMultipleLicenseNamesAndUsesHashCatalogLookup() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "licenses": {
                "abc": {"name": "Hashed License", "url": "https://hash.example"},
                "Apache-2.0": {"name": "Apache License 2.0"}
              },
              "libraries": [
                {
                  "name": "Multi",
                  "licenses": [
                    {"hash": "abc"},
                    "Apache-2.0"
                  ]
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals("Hashed License, Apache License 2.0", items.single().summary)
        assertTrue(items.single().detail.contains("Hashed License\nhttps://hash.example"))
        assertTrue(items.single().detail.contains("Apache License 2.0"))
    }

    @Test
    fun scmConnectionAndOrganisationUrlAreLastResorts() {
        val items = OpenSourceLicenseCatalog.parseLibraryItems(
            """
            {
              "libraries": [
                {
                  "name": "scm-connection",
                  "scm": {"connection": "https://conn.example"},
                  "licenses": []
                },
                {
                  "name": "org-alt",
                  "organization": {"organisationUrl": "https://org.example/alt"},
                  "licenses": []
                }
              ]
            }
            """.trimIndent(),
            labels,
        )

        assertEquals("https://conn.example", items.first { it.name == "scm-connection" }.website)
        assertEquals("https://org.example/alt", items.first { it.name == "org-alt" }.website)
    }
}
