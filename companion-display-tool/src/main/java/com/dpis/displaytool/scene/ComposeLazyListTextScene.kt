package com.dpis.displaytool.scene

import android.widget.TextView
import com.dpis.displaytool.CompanionContract

class ComposeLazyListTextScene : DisplayScene {
    override fun id(): String = "compose_lazy_list_text"

    override fun supportsVariant(variant: String): Boolean {
        return CompanionContract.VARIANT_NORMAL == variant
    }

    override fun create(runtime: SceneRuntime, variant: String): ScenePresentation {
        val textView = TextView(runtime.activity())
        textView.text = "Compose lazy list text placeholder"
        TextSceneSupport.applySp(textView, TextSceneSupport.BASE_SP)
        return ScenePresentation.view(
            textView,
            textView,
            TextSceneSupport.BASE_SP,
            "compose_text_primary",
            "compose_lazy_first_screen_stable"
        )
    }
}
