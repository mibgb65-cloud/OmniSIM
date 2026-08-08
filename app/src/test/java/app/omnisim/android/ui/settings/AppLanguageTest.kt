package app.omnisim.android.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun `empty language tag follows system`() {
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.System, AppLanguage.fromLanguageTag(""))
    }

    @Test
    fun `Chinese language tags select simplified Chinese`() {
        assertEquals(AppLanguage.SimplifiedChinese, AppLanguage.fromLanguageTag("zh-CN"))
        assertEquals(AppLanguage.SimplifiedChinese, AppLanguage.fromLanguageTag("zh-Hans-CN"))
    }

    @Test
    fun `English language tags select English`() {
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en"))
        assertEquals(AppLanguage.English, AppLanguage.fromLanguageTag("en-US"))
    }
}
