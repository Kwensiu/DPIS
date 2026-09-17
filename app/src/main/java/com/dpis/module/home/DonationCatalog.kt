package com.dpis.module.home

import org.json.JSONObject
import java.math.BigDecimal

class DonationRecord(
    val id: String,
    val amount: BigDecimal,
    val amountText: String,
    val currency: String,
    val anonymous: Boolean,
    val supporterId: String?,
    val displayName: String?,
    val date: String?,
    val platform: String?,
    val orderRef: String?,
) {
    fun formattedAmount(): String = DonationCatalog.formatAmount(amountText, currency)

    fun groupingKey(): String? {
        if (anonymous) {
            return null
        }
        return supporterId ?: displayName
    }
}

/** Parses the donation ledger without Android resource access. */
internal object DonationCatalog {
    const val DEFAULT_CURRENCY = "CNY"
    const val AMOUNT_SUFFIX_CNY = "￥"

    private const val KEY_DONATIONS = "donations"
    private const val KEY_ID = "id"
    private const val KEY_AMOUNT = "amount"
    private const val KEY_CURRENCY = "currency"
    private const val KEY_ANONYMOUS = "anonymous"
    private const val KEY_SUPPORTER_ID = "supporterId"
    private const val KEY_DISPLAY_NAME = "displayName"
    private const val KEY_DATE = "date"
    private const val KEY_PLATFORM = "platform"
    private const val KEY_ORDER_REF = "orderRef"

    fun parseRecords(rawJson: String): List<DonationRecord> {
        val root = JSONObject(rawJson)
        val donations = root.optJSONArray(KEY_DONATIONS) ?: return emptyList()
        val records = ArrayList<DonationRecord>(donations.length())
        val seenIds = HashSet<String>()
        for (index in 0 until donations.length()) {
            val item = donations.optJSONObject(index) ?: continue
            val record = parseRecord(item) ?: continue
            if (!seenIds.add(record.id)) {
                continue
            }
            records.add(record)
        }
        return records
    }

    fun formatAmount(amount: BigDecimal, currency: String = DEFAULT_CURRENCY): String =
        formatAmount(amount.toPlainString(), currency)

    fun formatAmount(amountText: String, currency: String = DEFAULT_CURRENCY): String {
        return if (currency == DEFAULT_CURRENCY) {
            amountText + AMOUNT_SUFFIX_CNY
        } else {
            "$amountText $currency"
        }
    }

    private fun parseRecord(item: JSONObject): DonationRecord? {
        val id = optionalText(item, KEY_ID) ?: return null
        val amountValue = item.opt(KEY_AMOUNT)
        if (amountValue !is String) {
            return null
        }
        val amountText = amountValue.trim()
        val amount = parsePositiveAmount(amountText) ?: return null
        val displayName = optionalText(item, KEY_DISPLAY_NAME)
        val supporterId = optionalText(item, KEY_SUPPORTER_ID)
        val anonymous = item.optBoolean(KEY_ANONYMOUS, false) ||
                (displayName == null && supporterId == null)
        return DonationRecord(
            id = id,
            amount = amount,
            amountText = amountText,
            currency = optionalText(item, KEY_CURRENCY) ?: DEFAULT_CURRENCY,
            anonymous = anonymous,
            supporterId = supporterId,
            displayName = displayName,
            date = optionalText(item, KEY_DATE),
            platform = optionalText(item, KEY_PLATFORM),
            orderRef = optionalText(item, KEY_ORDER_REF),
        )
    }

    private fun parsePositiveAmount(raw: String): BigDecimal? {
        val amount = try {
            BigDecimal(raw)
        } catch (_: NumberFormatException) {
            return null
        }
        return amount.takeIf { it.signum() > 0 }
    }

    private fun optionalText(item: JSONObject, key: String): String? {
        if (!item.has(key) || item.isNull(key)) {
            return null
        }
        val value = item.opt(key)
        if (value !is String) {
            return null
        }
        return value.trim().takeIf { it.isNotEmpty() }
    }
}
