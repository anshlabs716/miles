package com.example.miles.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.miles.data.local.AppIconOption

class AppIconManager(private val context: Context) {

    companion object {
        private const val TAG = "AppIconManager"
    }

    /**
     * Resolves the proper ComponentName for an AppIconOption alias.
     * Checks both the declared aliasClass and alternative package mappings.
     */
    private fun resolveComponentName(option: AppIconOption): ComponentName {
        val simpleName = option.aliasClass.substringAfterLast('.')
        // 1. Try manifest-relative namespace class name
        val candidate1 = ComponentName(context.packageName, option.aliasClass)
        // 2. Try namespace with package name
        val candidate2 = ComponentName(context.packageName, "${context.packageName}.$simpleName")

        val pm = context.packageManager
        return try {
            pm.getComponentEnabledSetting(candidate1)
            candidate1
        } catch (_: Exception) {
            try {
                pm.getComponentEnabledSetting(candidate2)
                candidate2
            } catch (_: Exception) {
                // Default to candidate1
                candidate1
            }
        }
    }

    /**
     * Enables the selected icon alias and disables all other aliases.
     * Returns true if the operation executed without fatal exceptions.
     */
    fun setAppIcon(option: AppIconOption): Boolean {
        val packageManager = context.packageManager
        var anySuccess = false

        AppIconOption.entries.forEach { icon ->
            val isTarget = icon == option
            val desiredState = if (isTarget) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }

            val targetComponent = resolveComponentName(icon)
            try {
                val currentState = packageManager.getComponentEnabledSetting(targetComponent)
                if (currentState != desiredState) {
                    packageManager.setComponentEnabledSetting(
                        targetComponent,
                        desiredState,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "Switched ${targetComponent.className} -> $desiredState")
                }
                if (isTarget) anySuccess = true
            } catch (e: Exception) {
                Log.w(TAG, "Could not set component ${targetComponent.className}: ${e.message}")
            }
        }
        return anySuccess
    }

    /**
     * Inspects PackageManager to find which alias is currently enabled.
     */
    fun getActiveAppIcon(): AppIconOption {
        val pm = context.packageManager
        for (icon in AppIconOption.entries) {
            val component = resolveComponentName(icon)
            try {
                val state = pm.getComponentEnabledSetting(component)
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    return icon
                }
            } catch (_: Exception) {
                // Skip unresolvable
            }
        }
        return AppIconOption.DEFAULT
    }

    /**
     * Refreshes and returns the currently verified active icon in Android OS.
     */
    fun refreshLauncherStatus(): Pair<AppIconOption, String> {
        val active = getActiveAppIcon()
        val totalAliases = AppIconOption.entries.size
        return Pair(active, "Package manager verified: $totalAliases aliases registered, active: ${active.label}")
    }
}
