package com.dpis.module.updates.presentation

import android.content.Context
import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.RenderProps
import io.noties.markwon.core.CoreProps
import org.commonmark.node.Heading
import org.commonmark.node.ListItem
import org.commonmark.node.SoftLineBreak
import com.dpis.module.updates.insertVisibleListMarkers
import com.dpis.module.updates.headingScale
import com.dpis.module.updates.ReleaseNotesListMarker

internal fun renderReleaseNotesWithMarkwon(context: Context, markdown: String): CharSequence {
    val rendered = Markwon.builder(context)
        .usePlugin(ReleaseNotesComposeCompatiblePlugin())
        .build()
        .toMarkdown(markdown)
    return insertVisibleListMarkers(rendered)
}

internal fun listMarkerText(props: RenderProps): String {
    return if (CoreProps.LIST_ITEM_TYPE.get(props) == CoreProps.ListItemType.ORDERED) {
        val number = CoreProps.ORDERED_LIST_ITEM_NUMBER.get(props) ?: 1
        "$number. "
    } else {
        "• "
    }
}

private class ReleaseNotesComposeCompatiblePlugin : AbstractMarkwonPlugin() {
    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(Heading::class.java) { _, props ->
            val level = CoreProps.HEADING_LEVEL.require(props)
            arrayOf<Any>(StyleSpan(Typeface.BOLD), RelativeSizeSpan(headingScale(level)))
        }
        builder.setFactory(ListItem::class.java) { _, props ->
            ReleaseNotesListMarker(listMarkerText(props))
        }
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(SoftLineBreak::class.java) { visitor, _ ->
            visitor.builder().append('\n')
        }
    }
}
