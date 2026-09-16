package com.example.miles.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.miles.data.local.AppIconOption

/** Controls the launcher aliases declared in AndroidManifest.xml. */
class AppIconManager(private val context: Context) {
    companion object { private const val TAG = "AppIconManager" }

    private val packageManager: PackageManager
        get() = context.packageManager

    /**
     * Finds the real declared component. The AI Studio project uses an
     * applicationId different from its Kotlin/manifest namespace, so simply
     * concatenating context.packageName with the alias class is not reliable.
     */
    private fun resolveComponentName(option: AppIconOption): ComponentName {
        val simpleName = option.aliasClass.substringAfterLast('.')
        val candidates = listOf(
            ComponentName(context.packageName, option.aliasClass),
            ComponentName(context.packageName, "${context.packageName}.$simpleName")
        )
        for (candidate in candidates) {
            val exists = runCatching {
                // Must pass MATCH_DISABLED_COMPONENTS so disabled aliases are recognized
                packageManager.getActivityInfo(candidate, PackageManager.MATCH_DISABLED_COMPONENTS)
                true
            }.getOrElse {
                runCatching {
                    // Fallback check using getComponentEnabledSetting
                    packageManager.getComponentEnabledSetting(candidate) >= 0
                }.getOrDefault(false)
            }
            if (exists) return candidate
        }
        return candidates.first()
    }

    /** Enables exactly one icon alias and disables every other icon alias. */
    fun setAppIcon(option: AppIconOption): Boolean {
        var targetEnabled = false
        AppIconOption.entries.forEach { icon ->
            val component = resolveComponentName(icon)
            val isTarget = (icon == option)
            val desiredState = if (isTarget) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            runCatching {
                packageManager.setComponentEnabledSetting(
                    component,
                    desiredState,
                    PackageManager.DONT_KILL_APP
                )
                if (isTarget) targetEnabled = true
            }.onFailure { error ->
                // Try alternate candidate package name in case namespace differs
                val altComponent = ComponentName(
                    context.packageName,
                    "${context.packageName}.${icon.aliasClass.substringAfterLast('.')}"
                )
                runCatching {
                    packageManager.setComponentEnabledSetting(
                        altComponent,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                    if (isTarget) targetEnabled = true
                }.onFailure { innerError ->
                    Log.e(TAG, "Could not update ${component.className}", innerError)
                }
            }
        }
        return targetEnabled
    }

    /** Reads the launcher state directly from PackageManager. */
    fun getActiveAppIcon(): AppIconOption {
        AppIconOption.entries.forEach { icon ->
            val component = resolveComponentName(icon)
            val state = runCatching {
                packageManager.getComponentEnabledSetting(component)
            }.getOrDefault(-1)
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return icon
            }
        }
        return AppIconOption.DEFAULT
    }

    /** Reconciles the manifest aliases and returns the verified active icon. */
    fun refreshLauncherStatus(): Pair<AppIconOption, String> {
        val active = getActiveAppIcon()
        setAppIcon(active)
        return Pair(active, "Active icon: ${active.label}")
    }
}
