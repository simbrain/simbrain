package org.simbrain.util.uisnapshot

import org.simbrain.util.genericframe.GenericJDialog
import org.simbrain.world.imageworld.ImageWorldComponent
import org.simbrain.world.imageworld.ImageWorldDesktopComponent
import org.simbrain.world.imageworld.dialogs.ImageProcessingPipelineDialog
import org.simbrain.world.imageworld.filters.EdgeDetectionFilter
import org.simbrain.world.imageworld.filters.GrayscaleOperation
import org.simbrain.world.imageworld.filters.ImageProcessingPipeline
import org.simbrain.world.imageworld.filters.ThresholdOperation
import java.awt.Component

class ImagePipelineDialogSnapshot : UiSnapshotDef {
    override val name = "image_pipeline_dialog"

    override fun build(): Component {
        val component = ImageWorldComponent("snapshot")
        val frame = GenericJDialog()
        val desktopComponent = ImageWorldDesktopComponent(frame, component)
        val pipeline = ImageProcessingPipeline("Demo Pipeline", component.world.imagePipelineCollection.imageSource).apply {
            addOperation(GrayscaleOperation())
            addOperation(ThresholdOperation())
            addOperation(EdgeDetectionFilter())
        }
        return ImageProcessingPipelineDialog(desktopComponent, pipeline)
    }
}
