package by.vsdev.tablet.demo.ui.di

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.ui.presentation.setup.SetupViewModel
import by.vsdev.tablet.demo.ui.presentation.table.TableViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule: Module =
    module {
        viewModel { SetupViewModel(get(), get()) }
        viewModel { (config: TableConfig, sessionId: String) ->
            TableViewModel(config, sessionId, get(), get(), get())
        }
    }
