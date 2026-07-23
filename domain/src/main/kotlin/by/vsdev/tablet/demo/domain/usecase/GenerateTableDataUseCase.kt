package by.vsdev.tablet.demo.domain.usecase

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.domain.model.TableDataResult
import by.vsdev.tablet.demo.domain.repository.TableDataRepository

class GenerateTableDataUseCase(
    private val repository: TableDataRepository,
) {
    suspend operator fun invoke(config: TableConfig): TableDataResult = repository.generate(config)
}
