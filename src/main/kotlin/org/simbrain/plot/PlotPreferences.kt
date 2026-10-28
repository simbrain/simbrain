/**
 * User preferences for plots: chart-wide styling (series palette, line width) that is read whenever a
 * chart is themed, and per-plot-type defaults that seed new plots. Plot models copy those defaults at
 * construction and serialize their own values, so changing a preference never alters a saved workspace.
 */
package org.simbrain.plot

import org.simbrain.util.*
import org.simbrain.util.propertyeditor.APETabOder
import java.awt.Color

@APETabOder("General", "Time series", "Heat map", "Raster", "Histogram", "Bar chart", "Pixel plot", "Projection")
object PlotPreferences: PreferenceHolder() {

    @UserParameter(
        label = "Series palette",
        description = "<html>The set of colors used to tell plotted data apart. The first line of a time series, " +
                "row of a raster plot,<br>histogram, or pie slice gets the first color, the next gets the second, " +
                "and so on.<br>Open time series and raster plots update immediately; histograms and pie charts " +
                "use it for newly created data.</html>",
        tab = "General",
        order = 10
    )
    var seriesPalette by EnumPreference(ChartSeriesPalette.TABLEAU_10)

    @UserParameter(
        label = "Line width",
        description = "Stroke width of time series lines",
        minimumValue = 0.5,
        increment = 0.5,
        tab = "General",
        order = 20
    )
    var lineWidth by DoublePreference(2.0)

    @UserParameter(
        label = "Fixed width",
        description = "If true, new time series only show the most recent time steps",
        tab = "Time series",
        order = 10
    )
    var timeSeriesFixedWidth by BooleanPreference(false)

    @UserParameter(
        label = "Window size",
        description = "Number of time steps shown by new time series when fixed width is used",
        minimumValue = 1.0,
        tab = "Time series",
        order = 20
    )
    var timeSeriesWindowSize by IntegerPreference(100)

    @UserParameter(
        label = "Recurrence color map",
        description = "How distances are mapped to colors in spectrum-mode recurrence plots",
        tab = "Time series",
        order = 30
    )
    var recurrenceColorMap by EnumPreference(ChartColorMap.JET)

    @UserParameter(
        label = "Color map",
        description = "How values are mapped to colors in new heat maps",
        tab = "Heat map",
        order = 10
    )
    var heatMapColorMap by EnumPreference(ChartColorMap.COOL_TO_HOT)

    @UserParameter(
        label = "Fixed width",
        description = "If true, new heat maps only keep the most recent time steps",
        tab = "Heat map",
        order = 20
    )
    var heatMapFixedWidth by BooleanPreference(true)

    @UserParameter(
        label = "Window size",
        description = "Number of time steps retained by new heat maps when fixed width is used",
        minimumValue = 1.0,
        tab = "Heat map",
        order = 30
    )
    var heatMapWindowSize by IntegerPreference(200)

    @UserParameter(
        label = "Dot size",
        description = "Size of dots in new raster plots",
        minimumValue = 1.0,
        tab = "Raster",
        order = 10
    )
    var rasterDotSize by IntegerPreference(4)

    @UserParameter(
        label = "Fixed width",
        description = "If true, new raster plots only show the most recent time steps",
        tab = "Raster",
        order = 20
    )
    var rasterFixedWidth by BooleanPreference(true)

    @UserParameter(
        label = "Window size",
        description = "Number of time steps shown by new raster plots when fixed width is used",
        minimumValue = 1.0,
        tab = "Raster",
        order = 30
    )
    var rasterWindowSize by IntegerPreference(100)

    @UserParameter(
        label = "Bins",
        description = "Number of bins in new histograms",
        minimumValue = 1.0,
        tab = "Histogram",
        order = 10
    )
    var histogramBins by IntegerPreference(25)

    @UserParameter(
        label = "Opacity",
        description = "Alpha (0-255) of histogram bars, so overlapping series blend",
        minimumValue = 0.0,
        maximumValue = 255.0,
        tab = "Histogram",
        order = 20
    )
    var histogramAlpha by IntegerPreference(0xB0)

    @UserParameter(
        label = "Invert brightness",
        description = "If true, new pixel plots map 0 to white and 1 to black",
        tab = "Pixel plot",
        order = 10
    )
    var pixelPlotInvertBrightness by BooleanPreference(true)

    @UserParameter(
        label = "Bar color",
        description = "Bar color of new bar charts",
        tab = "Bar chart",
        order = 10
    )
    var barColor by ColorPreference(ChartSeriesPalette.TABLEAU_10.colors[0])

    @UserParameter(
        label = "Hot color",
        description = "Color of the current point in new projection plots",
        tab = "Projection",
        order = 10
    )
    var projectionHotColor by ColorPreference(Color.red)

    @UserParameter(
        label = "Base color",
        description = "Color of points that are not currently highlighted in new projection plots, per light/dark theme",
        tab = "Projection",
        order = 20
    )
    var projectionBaseColor by ThemeColorPreference(ThemeColor(Color.DARK_GRAY))

    @UserParameter(
        label = "Coloring highlight color",
        description = "Color that frequently visited (Frequency) or likely next (Markov) points are shaded toward",
        tab = "Projection",
        order = 30
    )
    var projectionHighlightColor by ColorPreference(Color.green)

    @UserParameter(
        label = "Show labels",
        description = "Show text labels sometimes associated with points",
        tab = "Projection",
        order = 40
    )
    var projectionShowLabels by BooleanPreference(true)

    @UserParameter(
        label = "Connect points",
        description = "Draw lines between points",
        tab = "Projection",
        order = 50
    )
    var projectionConnectPoints by BooleanPreference(false)

    @UserParameter(
        label = "Use hot point",
        description = "If true, the current point is rendered using the hot color",
        tab = "Projection",
        order = 60
    )
    var projectionUseHotPoint by BooleanPreference(true)
}
