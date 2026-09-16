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
     * Resolves the exact ComponentName recognized by Android PackageManager.
     * Checks all candidate package/class combinations.
     */
    fun resolveComponentName(option: AppIconOption): ComponentName {
        val simpleName = option.aliasClass.substringAfterLast('.')
        val candidates = listOf(
            ComponentName(context.packageName, "com.example.$simpleName"),
            ComponentName(context.packageName, "${context.packageName}.$simpleName"),
            ComponentName(context.packageName, option.aliasClass),
            ComponentName(context, option.aliasClass)
        )
        for (cand in candidates) {
            val exists = runCatching {
                val state = packageManager.getComponentEnabledSetting(cand)
                state >= 0 // Valid Android component state (0=default, 1=enabled, 2=disabled)
            }.getOrDefault(false)
            if (exists) return cand
        }
        return candidates.first()
    }

    /**
     * Enables exactly one icon alias and disables all other icon aliases.
     * CRITICAL: Enables the target alias FIRST before disabling others so
     * the launcher never observes 0 active launcher components.
     */
    fun setAppIcon(option: AppIconOption): Boolean {
        val targetComponent = resolveComponentName(option)
        var targetSuccess = false

        // 1. Enable target component FIRST
        runCatching {
            packageManager.setComponentEnabledSetting(
                targetComponent,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            targetSuccess = true
            Log.d(TAG, "Successfully enabled target launcher component: ${targetComponent.className}")
        }.onFailure { error ->
            Log.e(TAG, "Failed to enable target component: ${targetComponent.className}", error)
        }

        // 2. Disable all other launcher components
        AppIconOption.entries.forEach { icon ->
            if (icon != option) {
                val otherComponent = resolveComponentName(icon)
                runCatching {
                    packageManager.setComponentEnabledSetting(
                        otherComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }.onFailure { error ->
                    Log.w(TAG, "Could not disable component: ${otherComponent.className}", error)
                }
            }
        }

        return targetSuccess
    }

    /** Reads the launcher state directly from PackageManager. */
    fun getActiveAppIcon(): AppIconOption {
        for (icon in AppIconOption.entries) {
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
