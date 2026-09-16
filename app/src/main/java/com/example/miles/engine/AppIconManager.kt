package com.example.miles.engine

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.example.miles.data.local.AppIconOption

/** Controls the launcher aliases declared in AndroidManifest.xml. */
class AppIconManager(private val context: Context) {
    companion object { private const val TAG = "AppIconManager" }

    private val packageManager: PackageManager get() = context.packageManager

    private fun candidateNames(option: AppIconOption): List<ComponentName> {
        val simpleName = option.aliasClass.substringAfterLast('.')
        return listOf(
            ComponentName(context.packageName, "com.example.$simpleName"),
            ComponentName(context.packageName, "${context.packageName}.$simpleName"),
            ComponentName(context.packageName, option.aliasClass)
        ).distinct()
    }

    /** Resolve a manifest alias by asking PackageManager whether the activity actually exists. */
    fun resolveComponentName(option: AppIconOption): ComponentName {
        candidateNames(option).forEach { candidate ->
            if (runCatching { packageManager.getActivityInfo(candidate, 0) }.isSuccess) {
                return candidate
            }
        }
        throw IllegalStateException("Launcher alias ${option.aliasClass} is not declared in the installed APK")
    }

    /** Enables exactly one real launcher alias and disables the other aliases. */
    fun setAppIcon(option: AppIconOption): Boolean {
        val target = runCatching { resolveComponentName(option) }.getOrElse {
            Log.e(TAG, "Target launcher alias missing", it)
            return false
        }

        // Enable the new alias first so the launcher never sees zero active aliases.
        runCatching {
            packageManager.setComponentEnabledSetting(
                target,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }.onFailure {
            Log.e(TAG, "Failed to enable ${target.className}", it)
            return false
        }

        AppIconOption.entries.filter { it != option }.forEach { icon ->
            runCatching {
                val component = resolveComponentName(icon)
                packageManager.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure { Log.w(TAG, "Could not disable ${icon.aliasClass}", it) }
        }
        Log.d(TAG, "Active launcher icon: ${target.className}")
        return true
    }

    fun getActiveAppIcon(): AppIconOption {
        AppIconOption.entries.forEach { icon ->
            val enabled = runCatching {
                packageManager.getComponentEnabledSetting(resolveComponentName(icon)) ==
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            }.getOrDefault(false)
            if (enabled) return icon
        }
        return AppIconOption.DEFAULT
    }

    fun refreshLauncherStatus(): Pair<AppIconOption, String> {
        val active = getActiveAppIcon()
        val applied = setAppIcon(active)
        return if (applied) Pair(active, "Active icon: ${active.label}")
        else Pair(AppIconOption.DEFAULT, "Launcher icon aliases unavailable")
    }
}
