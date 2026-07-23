package by.vsdev.tablet.demo.di

import android.util.Log
import by.vsdev.tablet.demo.data.di.dataModule
import by.vsdev.tablet.demo.data.recovery.FileTableRecoveryRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
import by.vsdev.tablet.demo.recovery.RecoveryFailureReporter
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

private val domainModule =
    module {
        factory { ValidateTableConfigUseCase() }
        factory { GenerateTableDataUseCase(get()) }
    }

private val recoveryModule =
    module {
        single {
            RecoveryFailureReporter { operation, sessionId, error ->
                Log.w(
                    "TableRecovery",
                    "$operation failed${sessionId?.let { " for session $it" }.orEmpty()}",
                    error,
                )
            }
        }
        single<TableRecoveryRepository> {
            FileTableRecoveryRepository(
                directory = File(androidContext().noBackupFilesDir, "table-recovery"),
                ioDispatcher = get(),
                failureReporter = get(),
            )
        }
    }

internal val appModules = listOf(dataModule, domainModule, recoveryModule, uiModule)
