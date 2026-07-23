package by.vsdev.tablet.demo

import android.content.Context
import android.content.res.Configuration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale
import by.vsdev.tablet.demo.ui.R as UiR

@RunWith(AndroidJUnit4::class)
class LocalizationTest {
    @Test
    fun russianResourcesArePackaged() {
        val context = localizedContext("ru")

        assertEquals("Демо таблицы", context.getString(R.string.app_name))
        assertEquals("Создать таблицу", context.getString(UiR.string.setup_build))
        assertEquals("Повторить", context.getString(UiR.string.table_retry))
    }

    @Test
    fun belarusianResourcesArePackaged() {
        val context = localizedContext("be")

        assertEquals("Дэма табліцы", context.getString(R.string.app_name))
        assertEquals("Стварыць табліцу", context.getString(UiR.string.setup_build))
        assertEquals("Паўтарыць", context.getString(UiR.string.table_retry))
    }

    private fun localizedContext(languageTag: String): Context {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(targetContext.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return targetContext.createConfigurationContext(configuration)
    }
}
