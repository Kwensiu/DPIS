package com.dpis.module.runtime.systemserver.selection

import android.content.pm.ActivityInfo
import com.dpis.module.config.PerAppDisplayConfig
import com.dpis.module.config.PerAppDisplayConfigSource
import com.dpis.module.runtime.systemserver.diagnostics.HookLog
import com.dpis.module.runtime.systemserver.hook.EntryRoute
import com.dpis.module.runtime.systemserver.mutation.RelayoutWindow
import com.dpis.module.runtime.webapk.WebApkCarrierResolver
import java.util.Objects
import java.util.function.Predicate

/**
 * Which configured package a system_server hook argument belongs to.
 * Text inside a window object is not enough to select another app during
 * relayout or display-policy layout.
 */
object ConfiguredPackage {
    fun interface Lookup {
        fun find(packageName: String?): PerAppDisplayConfig?
    }

    class Resolved(
        @JvmField val packageName: String?,
        @JvmField val config: PerAppDisplayConfig?,
        @JvmField val candidatePackagesSummary: String?,
        @JvmField val fallbackFromPackage: String?,
    )

    @JvmStatic
    fun resolve(self: Any?, args: List<*>?, lookup: Lookup?): Resolved =
        resolve(self, args, lookup, true)

    @JvmStatic
    fun resolveForEntry(
        entryName: String?,
        self: Any?,
        args: List<*>?,
        lookup: Lookup?,
    ): Resolved = resolve(self, args, lookup, allowsTextFallback(entryName))

    @JvmStatic
    fun resolve(
        self: Any?,
        args: List<*>?,
        lookup: Lookup?,
        allowTextFallback: Boolean,
    ): Resolved {
        val argumentList: List<*> = args ?: emptyList<Any?>()
        val primaryPackage = PackageNames.find(self, argumentList)
        val candidatePackages = LinkedHashSet<String>()
        if (primaryPackage != null) {
            candidatePackages.add(primaryPackage)
        }
        if (allowTextFallback) {
            candidatePackages.addAll(
                HookLog.collectTextPackages(self, argumentList, 6),
            )
        }
        val candidateSummary = HookLog.describePackageCandidates(candidatePackages)
        if (lookup == null) {
            return Resolved(primaryPackage, null, candidateSummary, null)
        }
        if (primaryPackage != null) {
            val primaryConfig = lookup.find(primaryPackage)
            if (primaryConfig != null) {
                return Resolved(primaryPackage, primaryConfig, candidateSummary, null)
            }
        }
        for (candidate in candidatePackages) {
            if (Objects.equals(candidate, primaryPackage)) {
                continue
            }
            val config = lookup.find(candidate)
            if (config != null) {
                return Resolved(candidate, config, candidateSummary, primaryPackage)
            }
        }
        return Resolved(primaryPackage, null, candidateSummary, null)
    }

    @JvmStatic
    fun activityInfoPackage(args: List<*>?): String? {
        if (args == null) {
            return null
        }
        for (arg in args) {
            if (arg is ActivityInfo && PackageNames.isLikely(arg.packageName)) {
                return arg.packageName
            }
        }
        return null
    }

    @JvmStatic
    fun launchPackage(lookup: Lookup?, primaryPackage: String?, textSource: Any?): String? {
        val webApkOwner = configuredWebApkOwner(lookup, HookLog.safeToString(textSource))
        return webApkOwner ?: primaryPackage
    }

    @JvmStatic
    fun resolveConfiguredPackageForTest(
        self: Any?,
        hasConfig: Predicate<String>?,
        vararg args: Any?,
    ): String? = resolve(self, listed(args), configWhen(hasConfig)).packageName

    @JvmStatic
    fun resolveConfiguredPackageForEntryForTest(
        entryName: String?,
        self: Any?,
        hasConfig: Predicate<String>?,
        vararg args: Any?,
    ): String? = resolveForEntry(entryName, self, listed(args), configWhen(hasConfig)).packageName

    @JvmStatic
    fun resolveRelayoutWindowPackageForTest(
        windowManagerService: Any?,
        values: List<*>?,
        hasConfig: Predicate<String>?,
    ): String? {
        val arguments = values ?: emptyList<Any?>()
        val target = RelayoutWindow.target(windowManagerService, arguments)
        val candidates = ArrayList<Any?>()
        candidates.add(windowManagerService)
        candidates.addAll(arguments)
        return resolve(target, candidates, configWhen(hasConfig)).packageName
    }

    @JvmStatic
    fun resolveLaunchActivityItemPackageForTest(
        primaryPackage: String?,
        hasConfig: Predicate<String>?,
        textSource: Any?,
    ): String? = launchPackage(configWhen(hasConfig), primaryPackage, textSource)

    @JvmStatic
    fun fromSource(source: PerAppDisplayConfigSource?, args: List<*>?): String? =
        launchPackage(
            if (source != null) Lookup { source.get(it) } else null,
            activityInfoPackage(args),
            args,
        )

    private fun listed(args: Array<out Any?>?): List<Any?> {
        val values = ArrayList<Any?>()
        if (args != null) {
            values.addAll(args)
        }
        return values
    }

    private fun configWhen(hasConfig: Predicate<String>?): Lookup = Lookup { packageName ->
        if (packageName == null || hasConfig == null || !hasConfig.test(packageName)) {
            null
        } else {
            PerAppDisplayConfig(packageName, 1)
        }
    }

    private fun allowsTextFallback(entryName: String?): Boolean {
        // Window layout/relayout objects may mention an app package while the
        // actual owner is SystemUI or another system window. Keep text fallback
        // for launch/config routes, but do not let hot window maintenance select
        // another app's config from toString() text alone.
        return !EntryRoute.isDisplayPolicyLayout(entryName) &&
                !EntryRoute.isRelayoutDispatch(entryName)
    }

    private fun configuredWebApkOwner(lookup: Lookup?, text: String?): String? {
        if (lookup == null || text.isNullOrEmpty()) {
            return null
        }
        for (owner in WebApkCarrierResolver.collectOwnerPackagesFromText(text, 4)) {
            if (ConfigSelection.selectConfigForSystemServerEntry(
                    "launch-activity-item",
                    lookup.find(owner),
                ) != null
            ) {
                return owner
            }
        }
        return null
    }
}
