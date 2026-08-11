/**
 * Snapshot of [org.simbrain.plot.rasterchart.RasterPlotPanel]'s Swing legend strip: per-series
 * color swatch, name, and the muted per-series remove control that replaced the delete-last
 * button. Hover styling can't be captured here; only the resting state is the subject.
 */
package org.simbrain.util.uisnapshot

import org.simbrain.plot.raster.RasterModel
import org.simbrain.plot.rasterchart.RasterPlotPanel
import java.awt.Component
import java.util.function.Supplier

class RasterLegendSnapshot : UiSnapshotDef {
    override val name = "raster_legend"

    override fun build(): Component {
        var time = 0
        val model = RasterModel(Supplier { time })
        model.addDataSources(2)
        for (t in 0..60) {
            time = t
            model.rasterConsumerList[0].setValues(doubleArrayOf(
                if (t % 7 < 2) 1.0 else 0.0, 0.0, if (t % 5 == 0) 1.0 else 0.0
            ))
            model.rasterConsumerList[1].setValues(doubleArrayOf(
                0.0, if (t % 4 == 0) 1.0 else 0.0, 0.0, if (t % 9 < 3) 1.0 else 0.0
            ))
            model.rasterConsumerList[2].setValues(doubleArrayOf(
                0.0, 0.0, 0.0, 0.0, if (t % 3 == 0) 1.0 else 0.0
            ))
        }
        return RasterPlotPanel(model).also { it.init() }
    }
}
