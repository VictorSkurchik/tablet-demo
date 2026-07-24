package by.vsdev.tablet.demo.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

internal const val TARGET_PACKAGE = "by.vsdev.tablet.demo"
private const val SETUP_TIMEOUT_MILLIS = 5_000L
private const val INPUT_TIMEOUT_MILLIS = 2_000L
private const val TABLE_TIMEOUT_MILLIS = 10_000L
private const val FLING_COUNT = 5
private const val SWIPE_STEPS = 8
private const val SWIPE_START_HEIGHT_NUMERATOR = 3
private const val SWIPE_HEIGHT_DENOMINATOR = 4
private const val FIRST_CELL_DESCRIPTION = "Row 1, column 1:"

internal fun MacrobenchmarkScope.buildMaximumTable() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    check(device.wait(Until.hasObject(By.text("Rows")), SETUP_TIMEOUT_MILLIS)) {
        "Setup screen did not appear"
    }

    val fields = device.findObjects(By.clazz("android.widget.EditText"))
    check(fields.size == 2) { "Expected two editable number fields, found ${fields.size}" }
    fields[0].text = "1000"
    check(
        device.wait(
            Until.hasObject(By.clazz("android.widget.EditText").text("1000")),
            INPUT_TIMEOUT_MILLIS,
        ),
    ) { "Rows input was not updated" }
    fields[1].text = "6"
    check(
        device.wait(
            Until.hasObject(By.clazz("android.widget.EditText").text("6")),
            INPUT_TIMEOUT_MILLIS,
        ),
    ) { "Columns input was not updated" }

    device.waitForIdle(INPUT_TIMEOUT_MILLIS)
    val buildButton = device.findObject(By.text("Build table")).parent
    check(buildButton.isEnabled && buildButton.isClickable) { "Build action did not become enabled" }
    buildButton.click()

    check(device.wait(Until.hasObject(By.text("Table · 1000 × 6")), TABLE_TIMEOUT_MILLIS)) {
        "Maximum table did not appear"
    }
    check(device.wait(Until.hasObject(By.descContains(FIRST_CELL_DESCRIPTION)), TABLE_TIMEOUT_MILLIS)) {
        "Maximum table cells did not finish loading"
    }
}

internal fun flingTable() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val firstCellSelector = By.descContains(FIRST_CELL_DESCRIPTION)
    val firstCell =
        checkNotNull(device.findObject(firstCellSelector)) {
            "The first table cell is not visible before scrolling"
        }
    val swipeX = firstCell.visibleBounds.centerX()
    repeat(FLING_COUNT) {
        device.swipe(
            swipeX,
            device.displayHeight * SWIPE_START_HEIGHT_NUMERATOR / SWIPE_HEIGHT_DENOMINATOR,
            swipeX,
            device.displayHeight / SWIPE_HEIGHT_DENOMINATOR,
            SWIPE_STEPS,
        )
    }
    check(device.wait(Until.gone(firstCellSelector), TABLE_TIMEOUT_MILLIS)) {
        "Table did not scroll: the first cell is still visible"
    }
}
