package by.vsdev.tablet.demo.di

import android.util.Log
import by.vsdev.tablet.demo.data.createRandomTableDataRepository
import by.vsdev.tablet.demo.data.recovery.FileTableRecoveryRepository
import by.vsdev.tablet.demo.data.recovery.RecoveryIoDispatcher
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.recovery.RecoveryFailureReporter
import by.vsdev.tablet.demo.recovery.RecoverySessionIdFactory
import by.vsdev.tablet.demo.recovery.TableRecoveryRepository
import by.vsdev.tablet.demo.recovery.UuidRecoverySessionIdFactory
import by.vsdev.tablet.demo.ui.di.uiModule
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

private val applicationModule =
    module {
        single { BackgroundDispatcher(Dispatchers.Default) }
        single<TableDataRepository> { createRandomTableDataRepository(get()) }
        factory { ValidateTableConfigUseCase() }
        factory { GenerateTableDataUseCase(get()) }
    }

private val recoveryModule =
    module {
        single { RecoveryIoDispatcher(Dispatchers.IO) }
        single<RecoverySessionIdFactory> { UuidRecoverySessionIdFactory() }
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

internal val appModules = listOf(applicationModule, recoveryModule, uiModule)
