package com.dpis.module.home

import java.math.BigDecimal

class DonationWallEntry(
    val id: String,
    val displayName: String?,
    val total: BigDecimal,
    val currency: String,
    val anonymous: Boolean,
    val donations: List<DonationRecord>,
) {
    fun formattedTotal(): String = DonationCatalog.formatAmount(total, currency)
}

/** Named supporters merge by id; indistinguishable receipts share one anonymous group. */
class DonationSupportWall(
    val namedSupporters: List<DonationWallEntry>,
    val anonymous: DonationWallEntry?,
) {
    companion object {
        const val ANONYMOUS_ID = "anonymous-bucket"

        val EMPTY = DonationSupportWall(emptyList(), null)

        fun from(records: List<DonationRecord>): DonationSupportWall {
            val named = LinkedHashMap<String, MutableList<DonationRecord>>()
            val anonymousRecords = mutableListOf<DonationRecord>()
            for (record in records) {
                val key = record.groupingKey()
                if (key == null) {
                    anonymousRecords += record
                } else {
                    named.getOrPut(key) { mutableListOf() }.add(record)
                }
            }
            val namedSupporters = named.map { (id, donations) ->
                wallEntry(
                    id = id,
                    anonymous = false,
                    donations = donations,
                )
            }.sortedWith(
                compareByDescending<DonationWallEntry> { it.total }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName.orEmpty() },
            )
            return DonationSupportWall(
                namedSupporters = namedSupporters,
                anonymous = anonymousRecords.takeIf { it.isNotEmpty() }?.let { donations ->
                    wallEntry(
                        id = ANONYMOUS_ID,
                        anonymous = true,
                        donations = donations,
                    )
                },
            )
        }

        private fun wallEntry(
            id: String,
            anonymous: Boolean,
            donations: List<DonationRecord>,
        ): DonationWallEntry {
            val ordered = sortDonations(donations)
            return DonationWallEntry(
                id = id,
                displayName = if (anonymous) {
                    null
                } else {
                    ordered.mapNotNull { it.displayName }.firstOrNull() ?: id
                },
                total = ordered.fold(BigDecimal.ZERO) { total, record -> total + record.amount },
                currency = ordered.firstOrNull()?.currency ?: DonationCatalog.DEFAULT_CURRENCY,
                anonymous = anonymous,
                donations = ordered,
            )
        }

        private fun sortDonations(donations: List<DonationRecord>): List<DonationRecord> =
            donations.sortedWith(
                compareByDescending<DonationRecord> { it.date != null }
                    .thenByDescending { it.date },
            )
    }
}
