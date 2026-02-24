package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.*
import org.simbrain.util.SmellSource
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.world.imageworld.filters.EdgeDetectionFilter
import org.simbrain.world.imageworld.filters.GrayscaleOperation
import org.simbrain.world.imageworld.filters.ResizeOperation
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.sensors.View3DSensor

/**
 * A demo to illustrate how to link 3d sensors to an image world for image processing.
 */
val view3dNavigationPipelineDemo = newSim {

    workspace.clearWorkspace()

    val odorWorldComponent = addOdorWorldComponent("Odor World")
    val odorWorld = odorWorldComponent.world.apply {
        tileMap = TileMap(20, 20)
        wrapAround = false
        isObjectsBlockMovement = false
    }

    odorWorld.tileMap.layers.first().let { layer ->
        for (x in 0 until odorWorld.tileMap.width) {
            for (y in 0 until odorWorld.tileMap.height) {
                layer[x, y] = 6
            }
        }
    }

    val agent = odorWorld.addEntity(
        odorWorld.width / 2,
        odorWorld.height / 2,
        EntityType.Amy
    ).apply {
        heading = 344.0
        location = point(54, 116)
        name = "Agent"
    }

    val view3dSensor = View3DSensor().apply {
        label = "3D View"
        fov = 90.0
        viewDistance = 450.0
        outputWidth = 100
        outputHeight = 100
        cameraWorldHeight = 16.0
        wallHeight = 2.0
        billboardSprites = true
    }
    agent.addSensor(view3dSensor)
    agent.addDefaultEffectors()

    // Add landmarks to create visually distinct viewpoints while navigating.
    listOf(
        Triple(120, 120, EntityType.Swiss),
        Triple(430, 120, EntityType.Fish),
        Triple(120, 430, EntityType.Candle),
        Triple(360, 300, EntityType.Pansy),
        Triple(430, 430, EntityType.Poison),
    ).forEach { (x, y, type) ->
        odorWorld.addEntity(x, y, type).apply {
            if (type == EntityType.Swiss) {
                smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            }
        }
    }

    // Selecting the agent affords that it is the one whose pov we are taking
    agent.select()

    val imageWorldComponent = addImageWorld("Pipeline View")
    val imageWorld = imageWorldComponent.world.apply {
        resetImageAlbum(100, 100)
        imagePipelineCollection.addPipeline("3D Edge 100x100") {
            addOperation(ResizeOperation(100, 100))
            addOperation(GrayscaleOperation())
            addOperation(EdgeDetectionFilter().apply {
                method = EdgeDetectionFilter.EdgeDetectionMethod.PREWITT
                threshold = 0.25
                gaussianSigma = 1.0
            })
        }
        setCurrentPipeline("3D Edge 100x100")
    }

    with(couplingManager) {
        createCoupling(
            view3dSensor.getProducer(view3dSensor::brightness),
            imageWorld.imageAlbum.getConsumer(imageWorld.imageAlbum::setBrightness)
        )
    }

    withGui {
        place(odorWorldComponent, 0, 0, 600, 600)
        place(imageWorldComponent, 605, 0, 600, 600)
    }

    workspace.updater.iterate(2)

    addSidebarInfo(
        """
        # 3D Navigation + Edge Pipeline

        This demo is meant to familiarize you with how [Image World](https://docs.simbrain.net/docs/worlds/imageworld.html) and
        **image processing pipelines** work using a live 3D sensor stream.

        ## What to do

        Run the simulation, and move the main agent around in the odor world either with keyboard or mouse, and observe the image world edge detection.
         
       In the image world you can edit the current pipeline and see how this changes the presentation. You can

        - Open the `Pipeline View` toolbar and edit the current pipeline.
        - Double click the edge detection operation to adjust its algorithm and parameters.
        - Observe how these changes affect processed output while you move.

        """.trimIndent()
    )
}
