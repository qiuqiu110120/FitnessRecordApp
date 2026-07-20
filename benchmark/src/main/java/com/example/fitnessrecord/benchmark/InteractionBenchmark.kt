package com.example.fitnessrecord.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class InteractionBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun switchTabs() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3
        ),
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForText("首页")
        }
    ) {
        device.clickText("AI建议")
        device.waitForText("AI 健身建议")
        device.clickText("设置")
        device.waitForText("大模型配置")
        device.clickText("首页")
        device.waitForText("FRA")
    }

    @Test
    fun pageCalendar() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(
            baselineProfileMode = BaselineProfileMode.Disable,
            warmupIterations = 3
        ),
        startupMode = StartupMode.WARM,
        iterations = 10,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            device.waitForText("FRA")
        }
    ) {
        device.findObject(By.desc("下一页")).click()
        device.waitForIdle()
        device.findObject(By.desc("上一页")).click()
        device.waitForIdle()
    }

    private fun UiDevice.clickText(text: String) {
        wait(Until.findObject(By.text(text)), FIND_TIMEOUT_MS)?.click()
            ?: error("Could not find text: $text")
    }

    private fun UiDevice.waitForText(text: String) {
        check(wait(Until.hasObject(By.text(text)), FIND_TIMEOUT_MS)) {
            "Could not find text: $text"
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.example.fitnessrecord"
        const val FIND_TIMEOUT_MS = 5_000L
    }
}
