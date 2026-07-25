package com.dpis.module.ui.compose

import android.view.View
import android.view.ViewGroup
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.core.view.ViewCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Temporary interop boundary for existing workspace renderers.
 *
 * Theme 1 may host an existing root view here while each workspace is migrated.
 * New reusable UI must be implemented in Compose instead of extending this bridge.
 */
@Composable
fun DpisLegacyWorkspaceHost(
    createView: (Context) -> View,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            createView(context).also { legacyRoot ->
                // AndroidView owns the View after this one-time factory handoff. Detaching here
                // avoids mutating the View tree from composition during later recompositions.
                detachFromCurrentParent(legacyRoot)
                legacyRoot.addOnAttachStateChangeListener(
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(view: View) {
                            // The root was detached from Activity content before AndroidView
                            // reattached it. Compose may have already consumed the request,
                            // so dispatch the root's actual insets to legacy listeners.
                            dispatchRootInsets(view)
                        }

                        override fun onViewDetachedFromWindow(view: View) = Unit
                    }
                )
            }
        },
        update = { legacyRoot ->
            if (legacyRoot.isAttachedToWindow) {
                dispatchRootInsets(legacyRoot)
            }
        },
        modifier = modifier
    )
}

private fun detachFromCurrentParent(view: View) {
    (view.parent as? ViewGroup)?.removeView(view)
}

private fun dispatchRootInsets(view: View) {
    val rootInsets = ViewCompat.getRootWindowInsets(view)
    if (rootInsets != null) {
        ViewCompat.dispatchApplyWindowInsets(view, rootInsets)
        return
    }
    ViewCompat.requestApplyInsets(view)
}
