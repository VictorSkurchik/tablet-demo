package by.vsdev.tablet.demo.di

import by.vsdev.tablet.demo.data.di.dataModule
import by.vsdev.tablet.demo.domain.usecase.GenerateTableDataUseCase
import by.vsdev.tablet.demo.domain.usecase.ValidateTableConfigUseCase
import by.vsdev.tablet.demo.ui.di.uiModule
import org.koin.dsl.module

private val domainModule =
    module {
        factory { ValidateTableConfigUseCase() }
        factory { GenerateTableDataUseCase(get()) }
    }

internal val appModules = listOf(dataModule, domainModule, uiModule)
