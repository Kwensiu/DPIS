package com.dpis.module.home.presentation

import android.content.Context
import android.content.res.Resources
import com.dpis.module.R
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.home.DonationCatalog
import com.dpis.module.home.DonationSupportWall
import java.nio.charset.StandardCharsets

internal object DonationCatalogLoader {
    fun loadWall(context: Context): DonationSupportWall {
        return try {
            context.resources.openRawResource(R.raw.donations).use { inputStream ->
                val records = DonationCatalog.parseRecords(
                    String(inputStream.readBytes(), StandardCharsets.UTF_8),
                )
                DonationSupportWall.from(records)
            }
        } catch (error: Resources.NotFoundException) {
            DpisLog.e("Donate ledger resource missing", error)
            DonationSupportWall.EMPTY
        } catch (error: Throwable) {
            DpisLog.e("Donate ledger unreadable", error)
            DonationSupportWall.EMPTY
        }
    }
}
