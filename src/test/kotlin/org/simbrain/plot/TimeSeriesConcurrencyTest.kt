package org.simbrain.plot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.plot.timeseries.TimeSeriesModel
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities

class TimeSeriesConcurrencyTest {

    @Test
    fun `the series list can be read while another thread adds series`() {
        val model = TimeSeriesModel()
        var time = 0
        model.timeSupplier = { time }
        repeat(4) { model.addTimeSeries("Series $it") }

        val failure = AtomicReference<Throwable?>(null)
        // Stands in for the chart panel, which walks the list on the event thread on every repaint while the
        // workspace update thread is feeding the plot
        val reader = Thread {
            try {
                repeat(20_000) { model.timeSeriesList.minOfOrNull { series -> series.series.minY } }
            } catch (e: Throwable) {
                failure.compareAndSet(null, e)
            }
        }
        val writer = Thread {
            try {
                repeat(200) { model.addTimeSeries("Added $it") }
            } catch (e: Throwable) {
                failure.compareAndSet(null, e)
            }
        }
        reader.start()
        writer.start()
        reader.join()
        writer.join()

        assertNull(failure.get(), "Reading the series list raced with adding to it: ${failure.get()}")
    }

    @Test
    fun `setValues can populate an empty plot while the series list is being read`() {
        val model = TimeSeriesModel()
        var time = 0
        model.timeSupplier = { time }

        val failure = AtomicReference<Throwable?>(null)
        val reader = Thread {
            try {
                repeat(20_000) { model.timeSeriesList.map { series -> series.description } }
            } catch (e: Throwable) {
                failure.compareAndSet(null, e)
            }
        }
        reader.start()
        // setValues creates the series on first use, from whichever thread is running the couplings
        repeat(50) {
            time = it
            model.setValues(DoubleArray(6) { component -> component.toDouble() })
        }
        reader.join()

        assertNull(failure.get(), "Reading the series list raced with setValues populating it: ${failure.get()}")
        assertTrue(model.timeSeriesList.size == 6)
    }

    @Test
    fun `setValues reads one version of the series list while another thread replaces it`() {
        val model = TimeSeriesModel()
        model.timeSupplier = { 0 }
        repeat(6) { model.addTimeSeries("Series $it") }
        val series = model.timeSeriesList.toList()

        val failure = AtomicReference<Throwable?>(null)
        // Stands in for syncTimeSeries, which clears the list and refills it on the event thread
        val writer = Thread {
            try {
                repeat(5_000) {
                    SwingUtilities.invokeAndWait {
                        model.timeSeriesList.clear()
                        model.timeSeriesList.addAll(series)
                    }
                }
            } catch (e: Throwable) {
                failure.compareAndSet(null, e)
            }
        }
        writer.start()
        try {
            while (writer.isAlive) model.setValues(DoubleArray(6) { it.toDouble() })
        } catch (e: Throwable) {
            failure.compareAndSet(null, e)
        }
        writer.join()

        assertNull(failure.get(), "setValues raced with the series list being replaced: ${failure.get()}")
        assertEquals(series, model.timeSeriesList)
    }
}

