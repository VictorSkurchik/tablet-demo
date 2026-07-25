package by.vsdev.tablet.demo.di

import androidx.lifecycle.SavedStateHandle
import by.vsdev.tablet.demo.data.recovery.RecoveryIoDispatcher
import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {
    @Test
    fun `module graph is complete`() {
        productionModule()
            .verify(
                extraTypes =
                    listOf(
                        SavedStateHandle::class,
                        TableConfig::class,
                    ),
                injections =
                    injectedParameters(
                        definition<BackgroundDispatcher>(CoroutineDispatcher::class),
                        definition<RecoveryIoDispatcher>(CoroutineDispatcher::class),
                    ),
            )
    }

    private fun productionModule() = module { includes(*appModules.toTypedArray()) }
}
