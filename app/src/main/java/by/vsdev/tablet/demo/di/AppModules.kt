package by.vsdev.tablet.demo.di

import by.vsdev.tablet.demo.data.createRandomTableDataRepository
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import by.vsdev.tablet.demo.ui.di.uiModule
import kotlinx.coroutines.Dispatchers
import org.koin.dsl.module

private val applicationModule =
    module {
        single { BackgroundDispatcher(Dispatchers.Default) }
        single<TableDataRepository> { createRandomTableDataRepository(get()) }
        factory { ValidateTableConfigUseCase() }
        factory { GenerateTableDataUseCase(get()) }
    }

internal val appModules = listOf(applicationModule, uiModule)
