package by.vsdev.tablet.demo.data

import by.vsdev.tablet.demo.data.random.RandomStringGenerator
import by.vsdev.tablet.demo.domain.repository.TableDataRepository
import by.vsdev.tablet.demo.domain.util.BackgroundDispatcher
import kotlin.random.Random

fun createRandomTableDataRepository(backgroundDispatcher: BackgroundDispatcher): TableDataRepository =
    RandomTableDataRepository(
        backgroundDispatcher = backgroundDispatcher,
        stringGenerator = RandomStringGenerator(Random.Default),
    )
