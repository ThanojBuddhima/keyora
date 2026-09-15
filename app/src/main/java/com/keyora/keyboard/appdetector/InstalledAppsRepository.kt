package com.keyora.keyboard.appdetector

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class InstalledAppsRepository(
    private val packageManager: PackageManager
) {
    fun getLaunchableApps(): List<InstalledAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = runCatching {
                    info.loadLabel(packageManager).toString()
                }.getOrDefault(pkg)
                val icon = runCatching { info.loadIcon(packageManager) }.getOrNull()
                InstalledAppInfo(pkg, label, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun getAppLabel(packageName: String): String {
        return runCatching {
            val appInfo: ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    fun getAppIcon(packageName: String): Drawable? {
        return runCatching { packageManager.getApplicationIcon(packageName) }.getOrNull()
    }
}
