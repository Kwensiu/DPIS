package com.dpis.module.home

import com.dpis.module.SourceSmokeTestPaths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DonationSupportWallTest {
    @Test
    fun emptyRecordsProduceEmptyWall() {
        val wall = DonationSupportWall.from(emptyList())

        assertTrue(wall.namedSupporters.isEmpty())
        assertNull(wall.anonymous)
    }

    @Test
    fun namedDonationsMergeBySupporterIdAndAnonymousStaySeparateReceipts() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {"id": "a1", "supporterId": "tadow", "displayName": "@Tadow_", "amount": "6.66"},
                    {"id": "anon-1", "anonymous": true, "amount": "5.00"},
                    {"id": "a2", "supporterId": "tadow", "displayName": "@Tadow_", "amount": "1.00"},
                    {"id": "anon-2", "displayName": " ", "amount": "5.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(1, wall.namedSupporters.size)
        val named = wall.namedSupporters.single()
        assertEquals("tadow", named.id)
        assertEquals("@Tadow_", named.displayName)
        assertEquals(0, BigDecimal("7.66").compareTo(named.total))
        assertEquals(2, named.donations.size)
        assertEquals("7.66￥", named.formattedTotal())

        val anonymous = requireNotNull(wall.anonymous)
        assertTrue(anonymous.anonymous)
        assertEquals(DonationSupportWall.ANONYMOUS_ID, anonymous.id)
        assertNull(anonymous.displayName)
        assertEquals(2, anonymous.donations.size)
        assertEquals(0, BigDecimal("10.00").compareTo(anonymous.total))
        assertEquals(
            listOf("anon-1", "anon-2"),
            anonymous.donations.map { it.id },
        )
    }

    @Test
    fun sameDisplayNameWithoutSupporterIdMergesUntilIdsAreAssigned() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {"id": "one", "displayName": "@Same", "amount": "1.00"},
                    {"id": "two", "displayName": "@Same", "amount": "2.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(1, wall.namedSupporters.size)
        assertEquals("@Same", wall.namedSupporters.single().id)
        assertEquals(0, BigDecimal("3.00").compareTo(wall.namedSupporters.single().total))
    }

    @Test
    fun namedEntriesSortByTotalThenNameAndReceiptsPutDatedItemsFirst() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {"id": "low", "displayName": "@Zed", "amount": "1.00"},
                    {"id": "high-b", "supporterId": "alpha", "displayName": "@Beta", "amount": "3.00", "date": "2026-01-01"},
                    {"id": "high-a", "supporterId": "alpha", "displayName": "@Alpha", "amount": "3.00", "date": "2026-02-01"},
                    {"id": "high-none", "supporterId": "alpha", "displayName": "@Alpha", "amount": "3.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("alpha", "@Zed"), wall.namedSupporters.map { it.id })
        assertEquals("@Alpha", wall.namedSupporters[0].displayName)
        assertEquals(
            listOf("high-a", "high-b", "high-none"),
            wall.namedSupporters[0].donations.map { it.id },
        )
    }

    @Test
    fun anonymousFlagHidesANameFromTheNamedWall() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {"id": "hidden", "anonymous": true, "displayName": "@Secret", "amount": "8.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertTrue(wall.namedSupporters.isEmpty())
        assertEquals(1, wall.anonymous?.donations?.size)
        assertNull(wall.anonymous?.displayName)
    }

    @Test
    fun personNamedAnonymousIsNotTheAnonymousBucket() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {
                      "id": "named",
                      "displayName": "Anonymous",
                      "supporterId": "name-anonymous",
                      "amount": "5.00"
                    },
                    {"id": "hidden", "anonymous": true, "amount": "1.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(listOf("name-anonymous"), wall.namedSupporters.map { it.id })
        assertEquals("Anonymous", wall.namedSupporters.single().displayName)
        assertEquals(1, wall.anonymous?.donations?.size)
        assertEquals(DonationSupportWall.ANONYMOUS_ID, wall.anonymous?.id)
        assertNotEquals(wall.namedSupporters.single().id, wall.anonymous?.id)
    }

    @Test
    fun supporterIdAnonymousDoesNotReuseTheAnonymousBucketId() {
        val wall = DonationSupportWall.from(
            DonationCatalog.parseRecords(
                """
                {
                  "donations": [
                    {
                      "id": "named",
                      "displayName": "Anonymous",
                      "supporterId": "anonymous",
                      "amount": "5.00"
                    },
                    {"id": "hidden", "anonymous": true, "amount": "1.00"}
                  ]
                }
                """.trimIndent(),
            ),
        )

        val anonymous = requireNotNull(wall.anonymous)
        assertEquals("anonymous", wall.namedSupporters.single().id)
        assertEquals(DonationSupportWall.ANONYMOUS_ID, anonymous.id)
        assertNotEquals(wall.namedSupporters.single().id, anonymous.id)
    }

    @Test
    fun shippedLedgerPlacesEveryRecordOnTheWall() {
        val records = DonationCatalog.parseRecords(
            SourceSmokeTestPaths.read("src/main/res/raw/donations.json"),
        )
        val wall = DonationSupportWall.from(records)
        val wallIds = wall.namedSupporters.flatMap { entry -> entry.donations.map { it.id } } +
                (wall.anonymous?.donations?.map { it.id } ?: emptyList())

        assertEquals(records.map { it.id }.toSet(), wallIds.toSet())
    }
}
