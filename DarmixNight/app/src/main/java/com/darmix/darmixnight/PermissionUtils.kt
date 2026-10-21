package com.darmix.darmixnight

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat

data class PermissionStatus(
    val accessibilityEnabled: Boolean,
    val overlayGranted: Boolean,
    val batteryExempted: Boolean,
    val notificationsGranted: Boolean,
    val exactAlarmsGranted: Boolean
) {
    val allGranted: Boolean
        get() = accessibilityEnabled && overlayGranted && batteryExempted &&
                notificationsGranted && exactAlarmsGranted
}

object PermissionUtils {

    fun checkAll(context: Context): PermissionStatus {
        return PermissionStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            overlayGranted = Settings.canDrawOverlays(context),
            batteryExempted = isIgnoringBatteryOptimizations(context),
            notificationsGranted = areNotificationsGranted(context),
            exactAlarmsGranted = canScheduleExactAlarms(context)
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

    fun accessibilitySettingsIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    fun overlaySettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun batteryOptimizationIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun exactAlarmSettingsIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        )
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
}