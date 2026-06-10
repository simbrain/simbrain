package org.simbrain.util.uisnapshot

import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.JFreeChart
import org.jfree.chart.plot.PlotOrientation
import org.jfree.data.category.DefaultCategoryDataset
import org.jfree.data.general.DefaultPieDataset
import org.jfree.data.statistics.HistogramDataset
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import org.simbrain.plot.applySimbrainChartTheme
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.sin

/**
 * A 2x2 grid of the chart families Simbrain renders (XY line, bar, pie, histogram), each themed via
 * [applySimbrainChartTheme], so the chrome (backgrounds, gridlines, axis/tick/label text, legend)
 * can be inspected against light and dark. Series colors are synthetic — only the chrome is the
 * subject here.
 */
class ChartThemeSnapshot : UiSnapshotDef {
    override val name = "chart_theme"

    private fun lineChart(): JFreeChart {
        val series = XYSeries("sin")
        for (i in 0..60) series.add(i.toDouble(), sin(i / 6.0))
        val cos = XYSeries("cos")
        for (i in 0..60) cos.add(i.toDouble(), sin(i / 6.0 + 1.5) * 0.7)
        val data = XYSeriesCollection().apply { addSeries(series); addSeries(cos) }
        return ChartFactory.createXYLineChart("Time Series", "Time", "Value", data, PlotOrientation.VERTICAL, true, true, false)
            .apply { applySimbrainChartTheme() }
    }

    private fun barChart(): JFreeChart {
        val data = DefaultCategoryDataset().apply {
            addValue(3.0, "n", "A"); addValue(5.0, "n", "B"); addValue(2.0, "n", "C"); addValue(4.0, "n", "D")
        }
        return ChartFactory.createBarChart("Bar Chart", "Bar", "Value", data, PlotOrientation.VERTICAL, false, true, false)
            .apply { applySimbrainChartTheme() }
    }

    private fun pieChart(): JFreeChart {
        val data = DefaultPieDataset<String>().apply {
            setValue("Alpha", 40.0); setValue("Beta", 30.0); setValue("Gamma", 20.0); setValue("Delta", 10.0)
        }
        return ChartFactory.createPieChart("Pie Chart", data, true, true, false)
            .apply { applySimbrainChartTheme() }
    }

    private fun histogram(): JFreeChart {
        val values = DoubleArray(200) { sin(it / 5.0) + sin(it / 13.0) }
        val data = HistogramDataset().apply { addSeries("h", values, 20) }
        return ChartFactory.createHistogram("Histogram", "Value", "Count", data, PlotOrientation.VERTICAL, true, true, false)
            .apply { applySimbrainChartTheme() }
    }

    override fun build(): Component {
        lateinit var panel: JPanel
        SwingUtilities.invokeAndWait {
            panel = JPanel(GridLayout(2, 2, 8, 8)).apply {
                preferredSize = Dimension(720, 520)
                add(ChartPanel(lineChart()))
                add(ChartPanel(barChart()))
                add(ChartPanel(pieChart()))
                add(ChartPanel(histogram()))
            }
        }
        return panel
    }
}
