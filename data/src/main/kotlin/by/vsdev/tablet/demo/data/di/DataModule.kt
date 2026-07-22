package by.vsdev.tablet.demo.data.di

import by.vsdev.tablet.demo.data.RandomTableDataRepository
import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.random.Random

val dataModule: Module =
    module {
        single { BackgroundDispatcher(Dispatchers.Default) }
        single<Random> { Random.Default }
        single { RandomStringGenerator(get()) }
        single<TableDataRepository> { RandomTableDataRepository(get(), get()) }
    }
