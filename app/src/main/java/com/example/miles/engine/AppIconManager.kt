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
    private fun resolveComponentName(option: AppIconOption): ComponentName? {
        val candidates = listOf(
            ComponentName(context.packageName, option.aliasClass),
            ComponentName(
                context.packageName,
                "${context.packageName}.${option.aliasClass.substringAfterLast('.')}"
            )
        )
        return candidates.firstOrNull { candidate ->
            runCatching {
                packageManager.getActivityInfo(candidate, PackageManager.GET_META_DATA)
                true
            }.getOrDefault(false)
        }
    }

    /** Enables exactly one icon alias and disables every other icon alias. */
    fun setAppIcon(option: AppIconOption): Boolean {
        if (resolveComponentName(option) == null) {
            Log.e(TAG, "Launcher alias not found: ${option.aliasClass}")
            return false
        }

        var targetEnabled = false
        AppIconOption.entries.forEach { icon ->
            val component = resolveComponentName(icon) ?: return@forEach
            val desiredState = if (icon == option) {
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
                if (icon == option) targetEnabled = true
            }.onFailure { error ->
                Log.e(TAG, "Could not update ${component.className}", error)
            }
        }
        return targetEnabled
    }

    /** Reads the launcher state directly from PackageManager. */
    fun getActiveAppIcon(): AppIconOption {
        AppIconOption.entries.forEach { icon ->
            val component = resolveComponentName(icon) ?: return@forEach
            if (packageManager.getComponentEnabledSetting(component) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            ) {
                return icon
            }
        }
        return AppIconOption.DEFAULT
    }

    /** Reconciles the manifest aliases and returns the verified active icon. */
    fun refreshLauncherStatus(): AppIconOption {
        val active = getActiveAppIcon()
        setAppIcon(active)
        return getActiveAppIcon()
    }
}
