package com.dpis.module.runtime.webapk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Process
import com.dpis.module.config.DpisConfigStore
import com.dpis.module.config.RuntimePropertyConfigPreferences
import com.dpis.module.diagnostics.DpisLog
import com.dpis.module.runtime.hookapi.ModernApiCapabilities
import com.dpis.module.runtime.resources.ResourcesImplHookInstaller
import com.dpis.module.runtime.resources.ResourcesManagerHookInstaller
import com.dpis.module.runtime.systemserver.selection.PackageUidResolver
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class WebApkRuntimeOwnerBridge private constructor() {
    companion object {
        @JvmField
        val CHROME_PACKAGE = "com.android.chrome"
        private const val ACTIVITY_THREAD_CLASS = "android.app.ActivityThread"
        private const val WEBAPK_ACTIVITY_CLASS =
            "org.chromium.chrome.browser.webapps.SameTaskWebApkActivity"
        private const val HOOK_ID_ACTIVITY_ON_CREATE = "webapk_owner_activity_on_create"
        private const val HOOK_ID_ACTIVITY_ON_RESUME = "webapk_owner_activity_on_resume"
        private const val HOOK_ID_ACTIVITY_ON_NEW_INTENT = "webapk_owner_activity_on_new_intent"
        private const val HOOK_ID_ACTIVITY_THREAD_LAUNCH_PREFIX =
            "webapk_owner_activity_thread_launch"
        private val LAST_MESSAGES = ConcurrentHashMap<String, String>()
        @Volatile
        private var currentActivityThreadMethod: Method? = null
        @Volatile
        private var getActivitiesMethod: Method? = null
        @Volatile
        private var lifecycleHooksInstalled = false
        @Volatile
        private var activeOwnerPackage: String? = null

        @JvmStatic
        fun resolveEffectivePackage(
            store: DpisConfigStore?,
            carrierPackage: String?,
        ): String {
            if (CHROME_PACKAGE != carrierPackage || store == null) return carrierPackage ?: ""
            val owner = currentWebApkOwner()
            if (owner == null) {
                recordUnresolved()
                return carrierPackage
            }
            val ownerStore = ownerStore(owner, store)
            if (!hasActiveOwnerConfig(ownerStore, owner)) {
                logIfChanged(
                    "unconfigured:$owner",
                    "DPIS_WEBAPK Chrome carrier owner ignored: carrier=$carrierPackage, " +
                            "owner=$owner, reason=owner_not_configured",
                )
                return carrierPackage
            }
            logIfChanged(
                "owner:$owner",
                "DPIS_WEBAPK Chrome carrier using WebAPK owner: carrier=$carrierPackage, owner=$owner",
            )
            return owner
        }

        @JvmStatic
        fun resolveEffectiveStore(
            store: DpisConfigStore?,
            effectivePackage: String?,
        ): DpisConfigStore? = if (WebApkCarrierResolver.isWebApkOwnerPackage(effectivePackage)) {
            ownerStore(effectivePackage!!, store)
        } else {
            store
        }

        private fun hasActiveOwnerConfig(store: DpisConfigStore?, owner: String): Boolean {
            if (store == null || !store.isTargetDpisEnabled(owner)) return false
            val fontScalePercent = store.getTargetFontScalePercent(owner)
            return store.getTargetViewportSpec(owner).isEnabled ||
                    (fontScalePercent != null && fontScalePercent > 0 && fontScalePercent != 100) ||
                    !store.getTargetTypefaceId(owner).isNullOrBlank()
        }

        private fun ownerStore(owner: String, fallbackStore: DpisConfigStore?): DpisConfigStore {
            val ownerUserId = fallbackStore?.packageConfigOwnerUserId()
                ?: PackageUidResolver.userIdFromUid(Process.myUid())
                ?: throw IllegalStateException("Cannot resolve WebAPK runtime owner user")
            val runtimePreferences = RuntimePropertyConfigPreferences(
                owner,
                RuntimePropertyConfigPreferences.AutoViewportRuntimeRoute.ANY_ENABLED_TARGET,
                ownerUserId,
            )
            return DpisConfigStore(runtimePreferences, ownerUserId)
        }

        @JvmStatic
        fun installLifecycleHooks(
            xposed: XposedInterface?,
            carrierPackage: String?,
            apiCapabilities: ModernApiCapabilities,
        ) {
            if (WebApkCarrierResolver.isWebApkOwnerPackage(carrierPackage)) return
            if (CHROME_PACKAGE != carrierPackage || xposed == null || lifecycleHooksInstalled) return
            synchronized(WebApkRuntimeOwnerBridge::class.java) {
                if (lifecycleHooksInstalled) return
                try {
                    val onCreate =
                        Activity::class.java.getDeclaredMethod("onCreate", Bundle::class.java)
                    apiCapabilities.applyStableHookId(
                        xposed.hook(onCreate)
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                        HOOK_ID_ACTIVITY_ON_CREATE,
                    ).intercept { chain ->
                        observeActivity(chain.thisObject, null, "onCreate")
                        chain.proceed()
                    }
                    val onResume = Activity::class.java.getDeclaredMethod("onResume")
                    apiCapabilities.applyStableHookId(
                        xposed.hook(onResume)
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                        HOOK_ID_ACTIVITY_ON_RESUME,
                    ).intercept { chain ->
                        observeActivity(chain.thisObject, null, "onResume")
                        chain.proceed()
                    }
                    val onNewIntent =
                        Activity::class.java.getDeclaredMethod("onNewIntent", Intent::class.java)
                    apiCapabilities.applyStableHookId(
                        xposed.hook(onNewIntent)
                            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                        HOOK_ID_ACTIVITY_ON_NEW_INTENT,
                    ).intercept { chain ->
                        observeActivity(chain.thisObject, chain.getArg(0), "onNewIntent")
                        chain.proceed()
                    }
                    installActivityThreadLaunchHooks(xposed, apiCapabilities)
                    lifecycleHooksInstalled = true
                    DpisLog.i("DPIS_WEBAPK lifecycle bridge ready: carrier=$carrierPackage")
                } catch (throwable: Throwable) {
                    DpisLog.e(
                        "DPIS_WEBAPK lifecycle bridge failed: carrier=$carrierPackage",
                        throwable
                    )
                }
            }
        }

        @JvmStatic
        fun currentWebApkOwnerForTest(activityText: String?): String? =
            if (activityText == null || !activityText.contains(WEBAPK_ACTIVITY_CLASS)) {
                null
            } else {
                WebApkCarrierResolver.ownerPackageFromText(activityText)
            }

        @JvmStatic
        fun observeActivityForTest(
            activityClassName: String?,
            activityIntentText: String?,
            methodIntentText: String?,
            sourceTag: String?,
        ): String? {
            observeActivity(activityClassName, activityIntentText, methodIntentText, sourceTag)
            return activeOwnerPackage
        }

        @JvmStatic
        fun resetForHotReload() {
            activeOwnerPackage = null
            lifecycleHooksInstalled = false
            LAST_MESSAGES.clear()
        }

        @JvmStatic
        fun resetForTest() = resetForHotReload()

        @JvmStatic
        fun hasActiveOwnerConfigForTest(store: DpisConfigStore?, owner: String): Boolean =
            hasActiveOwnerConfig(store, owner)

        private fun currentWebApkOwner(): String? {
            val cachedOwner = activeOwnerPackage
            if (WebApkCarrierResolver.isWebApkOwnerPackage(cachedOwner)) return cachedOwner
            val activityThread = currentActivityThread() ?: return null
            var activities = invokeNoArg(activityThread, getActivitiesMethod(activityThread))
            if (activities == null) activities = readField(activityThread, "mActivities")
            if (activities == null) return null
            currentWebApkOwnerFromActivities(activities)?.let { return it }
            currentWebApkOwnerForTest(activities.toString())?.let { return it }
            return currentWebApkOwnerForTest(activityThread.toString())
        }

        private fun currentWebApkOwnerFromActivities(activities: Any?): String? {
            if (activities !is Map<*, *>) return null
            for (record in activities.values) {
                currentWebApkOwnerFromActivityRecord(record)?.let { return it }
            }
            return null
        }

        private fun currentWebApkOwnerFromActivityRecord(record: Any?): String? {
            if (isStopped(record)) return null
            var intent = readField(record, "intent")
            var owner = WebApkCarrierResolver.ownerPackageFromText(intent.toString())
            if (owner != null && isWebApkActivityRecord(record, intent)) return owner
            val activity = readField(record, "activity") ?: return null
            if (activity.javaClass.name != WEBAPK_ACTIVITY_CLASS) return null
            intent = invokeNoArg(activity, findNoArgMethod(activity.javaClass, "getIntent"))
            owner = WebApkCarrierResolver.ownerPackageFromText(intent.toString())
            return owner ?: WebApkCarrierResolver.ownerPackageFromText(record.toString())
        }

        private fun observeActivity(activityObject: Any?, methodIntent: Any?, sourceTag: String?) {
            val activity = activityObject as? Activity ?: return
            val observed = observeActivity(
                activity.javaClass.name,
                invokeNoArg(activity, findNoArgMethod(activity.javaClass, "getIntent")).toString(),
                methodIntent.toString(),
                sourceTag,
            )
            if (observed) syncActivityResources(activity, sourceTag)
        }

        private fun installActivityThreadLaunchHooks(
            xposed: XposedInterface,
            apiCapabilities: ModernApiCapabilities,
        ) {
            val activityThreadClass = Class.forName(ACTIVITY_THREAD_CLASS)
            for (method in activityThreadClass.declaredMethods) {
                val methodName = method.name
                if (methodName != "performLaunchActivity" && methodName != "handleLaunchActivity") continue
                apiCapabilities.applyStableHookId(
                    xposed.hook(method)
                        .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE),
                    "$HOOK_ID_ACTIVITY_THREAD_LAUNCH_PREFIX#${method.toGenericString()}",
                ).intercept { chain ->
                    observeLaunchArgs(methodName, chain.args)
                    chain.proceed()
                }
            }
        }

        private fun observeLaunchArgs(methodName: String, args: Iterable<*>?) {
            for (arg in args ?: return) {
                currentWebApkOwnerFromActivityRecord(arg)?.let {
                    cacheOwner(it, "launch owner cached: source=$methodName")
                    return
                }
            }
        }

        private fun observeActivity(
            activityClassName: String?,
            activityIntentText: String?,
            methodIntentText: String?,
            sourceTag: String?,
        ): Boolean {
            if (activityClassName == null) return false
            var owner = WebApkCarrierResolver.ownerPackageFromText(methodIntentText)
                ?: WebApkCarrierResolver.ownerPackageFromText(activityIntentText)
            if (WebApkCarrierResolver.isWebApkOwnerPackage(owner) && activityClassName.contains(".webapps.")) {
                return cacheOwner(
                    owner!!,
                    "lifecycle owner cached: source=$sourceTag, activity=$activityClassName"
                )
            }
            if (activityClassName != WEBAPK_ACTIVITY_CLASS) {
                clearActiveOwnerIfNeeded(activityClassName, sourceTag)
                return false
            }
            if (!WebApkCarrierResolver.isWebApkOwnerPackage(owner)) {
                logIfChanged(
                    "lifecycle-missing-owner:$sourceTag",
                    "DPIS_WEBAPK lifecycle owner missing: source=$sourceTag, activity=$activityClassName",
                )
                return false
            }
            return cacheOwner(owner!!, "lifecycle owner cached: source=$sourceTag")
        }

        private fun cacheOwner(owner: String, detail: String): Boolean {
            if (!WebApkCarrierResolver.isWebApkOwnerPackage(owner)) return false
            activeOwnerPackage = owner
            logIfChanged("owner-cache:$owner:$detail", "DPIS_WEBAPK $detail, owner=$owner")
            return true
        }

        private fun syncActivityResources(activity: Activity, sourceTag: String?) {
            val owner = activeOwnerPackage
            if (!WebApkCarrierResolver.isWebApkOwnerPackage(owner)) return
            try {
                val resources = activity.resources ?: return
                val ownerStore = ownerStore(owner!!, null)
                val config = resources.configuration
                ResourcesManagerHookInstaller.applyResourceOverrides(
                    config, ownerStore, owner, "WebApkOwnerBridge($sourceTag)",
                )
                val metrics = resources.displayMetrics
                ResourcesImplHookInstaller.applyDensityOverride(owner, config, metrics, ownerStore)
                logIfChanged(
                    "activity-sync:$owner:$sourceTag:${config.screenWidthDp}:${config.densityDpi}",
                    "DPIS_WEBAPK activity resources synced: owner=$owner, source=$sourceTag, " +
                            "widthDp=${config.screenWidthDp}, densityDpi=${config.densityDpi}",
                )
            } catch (throwable: Throwable) {
                DpisLog.e(
                    "DPIS_WEBAPK activity resources sync failed: owner=$owner, source=$sourceTag",
                    throwable
                )
            }
        }

        private fun clearActiveOwnerIfNeeded(activityClassName: String, sourceTag: String?) {
            val owner = activeOwnerPackage ?: return
            activeOwnerPackage = null
            logIfChanged(
                "lifecycle-clear:$activityClassName",
                "DPIS_WEBAPK lifecycle owner cleared: source=$sourceTag, owner=$owner, activity=$activityClassName",
            )
        }

        private fun isWebApkActivityRecord(record: Any?, intent: Any?): Boolean {
            val recordText = record.toString()
            val intentText = intent.toString()
            return recordText.contains(WEBAPK_ACTIVITY_CLASS) ||
                    intentText.contains("webapp://webapk-") ||
                    intentText.contains(WebApkCarrierResolver.WEBAPK_PACKAGE_EXTRA)
        }

        private fun isStopped(record: Any?): Boolean =
            readField(record, "stopped") as? Boolean ?: false

        private fun currentActivityThread(): Any? = try {
            var method = currentActivityThreadMethod
            if (method == null) {
                synchronized(WebApkRuntimeOwnerBridge::class.java) {
                    method = currentActivityThreadMethod
                    if (method == null) {
                        method = Class.forName(ACTIVITY_THREAD_CLASS)
                            .getDeclaredMethod("currentActivityThread")
                            .apply { isAccessible = true }
                        currentActivityThreadMethod = method
                    }
                }
            }
            method?.invoke(null)
        } catch (_: Throwable) {
            null
        }

        private fun getActivitiesMethod(activityThread: Any?): Method? {
            if (activityThread == null) return null
            return try {
                var method = getActivitiesMethod
                if (method == null) {
                    synchronized(WebApkRuntimeOwnerBridge::class.java) {
                        method = getActivitiesMethod
                        if (method == null) {
                            method = activityThread.javaClass.getDeclaredMethod("getActivities")
                                .apply { isAccessible = true }
                            getActivitiesMethod = method
                        }
                    }
                }
                method
            } catch (_: Throwable) {
                null
            }
        }

        private fun invokeNoArg(target: Any?, method: Method?): Any? = try {
            if (target == null || method == null) null else method.invoke(target)
        } catch (_: Throwable) {
            null
        }

        private fun findNoArgMethod(type: Class<*>?, methodName: String?): Method? = try {
            if (type == null || methodName == null) null else type.getMethod(methodName).apply {
                isAccessible = true
            }
        } catch (_: Throwable) {
            null
        }

        private fun readField(target: Any?, fieldName: String?): Any? = try {
            if (target == null || fieldName == null) null else target.javaClass
                .getDeclaredField(fieldName).apply { isAccessible = true }.get(target)
        } catch (_: Throwable) {
            null
        }

        private fun logIfChanged(key: String, message: String) {
            val previous = LAST_MESSAGES.put(key, message)
            if (message != previous) DpisLog.i(message)
        }

        private fun recordUnresolved() {
            logIfChanged(
                "chrome-owner-unresolved",
                "DPIS_WEBAPK Chrome carrier owner unresolved: activeOwner=$activeOwnerPackage, " +
                        "activityThreadAvailable=${currentActivityThread() != null}",
            )
        }
    }
}
