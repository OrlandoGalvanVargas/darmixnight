package com.darmix.darmixnight

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val batteryExempted: Boolean,
    val notificationsGranted: Boolean,
    val exactAlarmsGranted: Boolean,
    val restriction: RestrictionState = RestrictionState.NONE,
    val restrictionApplies: Boolean = false,
    val restrictionAcknowledgedByUser: Boolean = false,
    val xiaomiRequired: Boolean = false,
    val xiaomiDone: Boolean = false
) {
    val allGranted: Boolean
        get() = accessibilityEnabled && overlayGranted && batteryExempted &&
                notificationsGranted && exactAlarmsGranted

    val restrictionCleared: Boolean
        get() = !restrictionApplies ||
                restriction == RestrictionState.NONE ||
                accessibilityEnabled ||
                (restriction == RestrictionState.UNKNOWN && restrictionAcknowledgedByUser)

    val xiaomiSatisfied: Boolean
        get() = !xiaomiRequired || xiaomiDone

    val readyForMain: Boolean
        get() = allGranted && restrictionCleared && xiaomiSatisfied
}

enum class RestrictionState { NONE, BLOCKED, ACKNOWLEDGED, UNKNOWN }

object PermissionUtils {

    private const val OPSTR_ACCESS_RESTRICTED_SETTINGS = "android:access_restricted_settings"
    private const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS = "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"
    private const val SETTINGS_FRAGMENT_ARGS_KEY = ":settings:fragment_args_key"
    private const val SETTINGS_SHOW_FRAGMENT_ARGS = ":settings:show_fragment_args"

    fun checkAll(context: Context): PermissionStatus {
        val restriction = restrictedSettingsState(context)
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            overlayGranted = Settings.canDrawOverlays(context),
            batteryExempted = isIgnoringBatteryOptimizations(context),
            notificationsGranted = areNotificationsGranted(context),
            exactAlarmsGranted = canScheduleExactAlarms(context),
            restriction = restriction,
            restrictionApplies = restriction == RestrictionState.BLOCKED ||
                    restriction == RestrictionState.ACKNOWLEDGED ||
                    isInstalledFromFile(context),
            restrictionAcknowledgedByUser = SetupPrefs.isRestrictionAcknowledged(context),
            xiaomiRequired = isXiaomiFamily(),
            xiaomiDone = SetupPrefs.isXiaomiAutostartDone(context)
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = ComponentName(context, BlockAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServicesSetting)
        while (splitter.hasNext()) {
            val componentString = splitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentString)
            if (enabledComponent != null && enabledComponent == expectedComponent) {
                return true
            }
        }
        return false
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun areNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun restrictedSettingsState(context: Context): RestrictionState {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return RestrictionState.NONE
        val mode = try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.unsafeCheckOpNoThrow(
                OPSTR_ACCESS_RESTRICTED_SETTINGS,
                Process.myUid(),
                context.packageName
            )
        } catch (_: Exception) {
            return if (isInstalledFromFile(context)) RestrictionState.UNKNOWN else RestrictionState.NONE
        }
        return when (mode) {
            AppOpsManager.MODE_ALLOWED -> RestrictionState.NONE
            AppOpsManager.MODE_ERRORED -> RestrictionState.BLOCKED
            AppOpsManager.MODE_IGNORED -> RestrictionState.ACKNOWLEDGED
            AppOpsManager.MODE_DEFAULT ->
                if (isInstalledFromFile(context)) RestrictionState.BLOCKED else RestrictionState.NONE
            else -> RestrictionState.UNKNOWN
        }
    }

    private fun isInstalledFromFile(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return try {
            val source = context.packageManager.getInstallSourceInfo(context.packageName).packageSource
            source == PackageInstaller.PACKAGE_SOURCE_LOCAL_FILE ||
                    source == PackageInstaller.PACKAGE_SOURCE_DOWNLOADED_FILE
        } catch (_: Exception) {
            false
        }
    }

    fun isXiaomiFamily(): Boolean {
        val brand = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return listOf("xiaomi", "redmi", "poco").any { it in brand }
    }

    fun accessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun accessibilityServiceIntent(context: Context): Intent {
        val component = ComponentName(context, BlockAccessibilityService::class.java).flattenToString()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).putExtra(Intent.EXTRA_COMPONENT_NAME, component)
        } else {
            highlighted(accessibilitySettingsIntent(), component)
        }
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return highlighted(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ),
            context.packageName
        )
    }

    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun batteryOptimizationListIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    fun exactAlarmSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun notificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }

    fun appDetailsSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun xiaomiAutoStartIntent(context: Context): Intent {
        val intent = Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        val resolvable = intent.resolveActivity(context.packageManager) != null
        return if (resolvable) intent else appDetailsSettingsIntent(context)
    }

    fun open(context: Context, vararg intents: Intent) {
        val candidates = intents.toList() + appDetailsSettingsIntent(context)
        for (intent in candidates) {
            try {
                context.startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }

    private fun highlighted(intent: Intent, key: String): Intent {
        return intent.apply {
            putExtra(SETTINGS_FRAGMENT_ARGS_KEY, key)
            putExtra(SETTINGS_SHOW_FRAGMENT_ARGS, Bundle().apply { putString(SETTINGS_FRAGMENT_ARGS_KEY, key) })
        }
    }
}
