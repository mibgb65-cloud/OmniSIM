package app.omnisim.android.data.update

import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.omnisim.android.BuildConfig
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUpdateInstallerTest {
    @Test
    fun installedApkHasTheSamePackageAndSigner() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        assertTrue(
            hasMatchingUpdateSignature(context, File(context.applicationInfo.sourceDir)),
        )
    }

    @Test
    fun fileProviderExposesOnlyTheUpdateCacheFile() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val updateDirectory = File(context.cacheDir, "updates").apply { mkdirs() }
        val apk = File(updateDirectory, "OmniSIM-test-release.apk").apply {
            writeText("test APK placeholder")
        }
        try {
            assertFalse(hasMatchingUpdateSignature(context, apk))
            val uri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                apk,
            )
            assertEquals("content", uri.scheme)
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
            assertNotNull(content)
            assertTrue(content!!.contains("test APK"))
        } finally {
            apk.delete()
        }
    }
}
