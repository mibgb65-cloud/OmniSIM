package app.omnisim.android.data.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import app.omnisim.android.BuildConfig
import java.io.File
import java.security.MessageDigest

fun launchUpdateInstaller(context: Context, apkFile: File): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        !context.packageManager.canRequestPackageInstalls()
    ) {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:" + context.packageName),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return false
    }
    val apkUri = FileProvider.getUriForFile(
        context,
        BuildConfig.APPLICATION_ID + ".fileprovider",
        apkFile,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

@Suppress("DEPRECATION")
internal fun hasMatchingUpdateSignature(context: Context, apkFile: File): Boolean = runCatching {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }
    val installed = context.packageManager.getPackageInfo(context.packageName, flags)
    val update = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
        ?: return false
    if (update.packageName != context.packageName) return false
    val installedSigners = signerDigests(installed)
    val updateSigners = signerDigests(update)
    installedSigners.isNotEmpty() &&
        updateSigners.isNotEmpty() &&
        installedSigners.intersect(updateSigners).isNotEmpty()
}.getOrDefault(false)

@Suppress("DEPRECATION")
private fun signerDigests(info: PackageInfo): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = info.signingInfo ?: return emptySet()
        if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
    } else {
        info.signatures.orEmpty()
    }
    return signatures.mapTo(mutableSetOf()) { signature ->
        MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
