package by.vsdev.tablet.demo.data.recovery

import by.vsdev.tablet.demo.domain.model.TableConfig
import by.vsdev.tablet.demo.recovery.RecoveryFailureReporter
import by.vsdev.tablet.demo.recovery.RecoveryOperation
import by.vsdev.tablet.demo.recovery.model.RecoveredCell
import by.vsdev.tablet.demo.recovery.model.TableRecoverySnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class FileTableRecoveryRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `save load and delete have the expected lifecycle`() =
        runTest {
            val repository = repository()
            val snapshot = snapshot()

            repository.save(SESSION_ID, snapshot)

            assertEquals(snapshot, repository.load(SESSION_ID))
            assertTrue(temporaryFolder.root.resolve("$SESSION_ID.snapshot").isFile)
            assertFalse(temporaryFolder.root.resolve("$SESSION_ID.tmp").exists())

            repository.delete(SESSION_ID)

            assertNull(repository.load(SESSION_ID))
        }

    @Test
    fun `corrupt file is ignored and removed`() =
        runTest {
            val repository = repository()
            val file = temporaryFolder.root.resolve("$SESSION_ID.snapshot")
            file.writeText("not a snapshot")

            assertNull(repository.load(SESSION_ID))
            assertFalse(file.exists())
        }

    @Test
    fun `partial temporary file cannot replace a valid snapshot`() =
        runTest {
            val repository = repository()
            val snapshot = snapshot()
            assertTrue(repository.save(SESSION_ID, snapshot))
            temporaryFolder.root.resolve("$SESSION_ID.tmp").writeText("partial")

            assertEquals(snapshot, repository.load(SESSION_ID))
        }

    @Test
    fun `cleanup removes only expired recovery artifacts`() =
        runTest {
            val now = 10_000L
            val repository = repository(clockMillis = { now }, retentionMillis = 1_000L)
            val expired = temporaryFolder.root.resolve("expired.snapshot").apply { writeText("x") }
            val recent = temporaryFolder.root.resolve("recent.snapshot").apply { writeText("x") }
            val unrelated = temporaryFolder.root.resolve("keep.txt").apply { writeText("x") }
            expired.setLastModified(now - 1_001L)
            recent.setLastModified(now)
            unrelated.setLastModified(0L)

            repository.cleanupExpired()

            assertFalse(expired.exists())
            assertTrue(recent.exists())
            assertTrue(unrelated.exists())
        }

    @Test(expected = IllegalArgumentException::class)
    fun `unsafe session identifier is rejected`() =
        runTest {
            repository().load("../escape")
        }

    @Test
    fun `write failure is reported without crashing caller`() =
        runTest {
            val invalidDirectory = temporaryFolder.newFile("not-a-directory")
            val failures = mutableListOf<RecoveryOperation>()
            val repository =
                repository(
                    directory = invalidDirectory,
                    failureReporter =
                        RecoveryFailureReporter { operation, _, _ ->
                            failures += operation
                        },
                )

            assertFalse(repository.save(SESSION_ID, snapshot()))
            assertEquals(listOf(RecoveryOperation.SAVE), failures)
        }

    private fun repository(
        directory: java.io.File = temporaryFolder.root,
        clockMillis: () -> Long = { 0L },
        retentionMillis: Long = Long.MAX_VALUE,
        failureReporter: RecoveryFailureReporter = RecoveryFailureReporter { _, _, _ -> },
    ): FileTableRecoveryRepository =
        FileTableRecoveryRepository(
            directory = directory,
            ioDispatcher = RecoveryIoDispatcher(UnconfinedTestDispatcher()),
            failureReporter = failureReporter,
            clockMillis = clockMillis,
            retentionMillis = retentionMillis,
        )

    private fun snapshot(): TableRecoverySnapshot =
        TableRecoverySnapshot(
            config = TableConfig(1, 2),
            cells =
                listOf(
                    RecoveredCell("generated", false),
                    RecoveredCell("edited", true),
                ),
            editingIndex = 1,
            editorDraft = "draft",
        )

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
