package com.dpis.module.home

import com.dpis.module.SourceSmokeTestPaths
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class DonationCatalogTest {
    @Test
    fun missingDonationsKeyReturnsNoRecords() {
        assertTrue(DonationCatalog.parseRecords("{}").isEmpty())
    }

    @Test
    fun emptyDonationsArrayReturnsNoRecords() {
        assertTrue(DonationCatalog.parseRecords("""{"donations":[]}""").isEmpty())
    }

    @Test
    fun invalidJsonPropagatesParseFailure() {
        assertThrows(JSONException::class.java) {
            DonationCatalog.parseRecords("{")
        }
    }

    @Test
    fun skipsRecordsMissingIdAmountOrNonPositiveAmount() {
        val records = DonationCatalog.parseRecords(
            """
            {
              "donations": [
                {"amount": "10.00"},
                {"id": "no-amount"},
                {"id": "numeric-amount", "amount": 10.00},
                {"id": "zero", "amount": "0"},
                {"id": "negative", "amount": "-1.00"},
                {"id": "ok", "displayName": "@ok", "amount": "3.50"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, records.size)
        assertEquals("ok", records[0].id)
        assertEquals("3.50", records[0].amountText)
        assertEquals(0, BigDecimal("3.50").compareTo(records[0].amount))
    }

    @Test
    fun blankDisplayNameWithoutSupporterIdIsAnonymous() {
        val records = DonationCatalog.parseRecords(
            """
            {
              "donations": [
                {"id": "space", "displayName": " ", "amount": "5.00"},
                {"id": "flagged", "anonymous": true, "displayName": "@hidden", "amount": "1.00"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(2, records.size)
        assertTrue(records[0].anonymous)
        assertNull(records[0].displayName)
        assertTrue(records[1].anonymous)
        assertEquals("@hidden", records[1].displayName)
        assertNull(records[1].groupingKey())
        assertNull(records[0].groupingKey())
    }

    @Test
    fun supporterIdWithoutDisplayNameStaysNamed() {
        val record = DonationCatalog.parseRecords(
            """{"donations":[{"id":"a","supporterId":"person-a","amount":"10.00"}]}""",
        ).single()

        assertEquals(false, record.anonymous)
        assertEquals("person-a", record.groupingKey())
        assertNull(record.displayName)
    }

    @Test
    fun displayNameAnonymousWithSupporterIdStaysNamed() {
        val record = DonationCatalog.parseRecords(
            """
            {
              "donations": [
                {
                  "id": "a",
                  "displayName": "Anonymous",
                  "supporterId": "name-anonymous",
                  "amount": "5.00"
                }
              ]
            }
            """.trimIndent(),
        ).single()

        assertEquals(false, record.anonymous)
        assertEquals("name-anonymous", record.groupingKey())
        assertEquals("Anonymous", record.displayName)
    }

    @Test
    fun duplicateIdsKeepTheFirstRecord() {
        val records = DonationCatalog.parseRecords(
            """
            {
              "donations": [
                {"id": "same", "displayName": "@first", "amount": "1.00"},
                {"id": "same", "displayName": "@second", "amount": "2.00"}
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, records.size)
        assertEquals("@first", records[0].displayName)
    }

    @Test
    fun optionalLedgerFieldsStayNullWhenOmitted() {
        val record = DonationCatalog.parseRecords(
            """{"donations":[{"id":"a","displayName":"@a","amount":"10.00"}]}""",
        ).single()

        assertEquals(DonationCatalog.DEFAULT_CURRENCY, record.currency)
        assertNull(record.date)
        assertNull(record.platform)
        assertNull(record.orderRef)
        assertEquals("10.00￥", record.formattedAmount())
    }

    @Test
    fun optionalLedgerFieldsAreTrimmedWhenPresent() {
        val record = DonationCatalog.parseRecords(
            """
            {
              "donations": [
                {
                  "id": "a",
                  "displayName": " @a ",
                  "amount": " 6.66 ",
                  "date": " 2026-03-12 ",
                  "platform": " wechat ",
                  "orderRef": " ****7788 ",
                  "currency": " CNY "
                }
              ]
            }
            """.trimIndent(),
        ).single()

        assertEquals("@a", record.displayName)
        assertEquals("6.66", record.amountText)
        assertEquals("2026-03-12", record.date)
        assertEquals("wechat", record.platform)
        assertEquals("****7788", record.orderRef)
        assertEquals("CNY", record.currency)
    }

    @Test
    fun shippedLedgerIsParseable() {
        DonationCatalog.parseRecords(
            SourceSmokeTestPaths.read("src/main/res/raw/donations.json"),
        )
    }
}
