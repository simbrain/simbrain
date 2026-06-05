package org.simbrain.custom_sims.simulations.demos

import org.simbrain.custom_sims.SimulationScope
import org.simbrain.custom_sims.addOdorWorldComponent
import org.simbrain.util.SmellSource
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.point
import org.simbrain.world.odorworld.OdorWorldComponent
import org.simbrain.world.odorworld.behaviors.Wander
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.View3DSensor
import kotlin.math.atan2

data class View3dNavigationScene(
    val odorWorldComponent: OdorWorldComponent,
    val agent: OdorWorldEntity,
    val view3dSensor: View3DSensor
)

suspend fun SimulationScope.createView3dNavigationScene(
    outputWidth: Int,
    outputHeight: Int
): View3dNavigationScene {
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
        location = point(281, 300)
        name = "Agent"
        behavior = Wander().apply {
            maxSpeed = 1.5
        }
    }

    val view3dSensor = View3DSensor().apply {
        label = "3D View"
        fov = 90.0
        viewDistance = 450.0
        this.outputWidth = outputWidth
        this.outputHeight = outputHeight
        cameraWorldHeight = 16.0
        wallHeight = 2.0
        billboardSprites = true
    }
    agent.addSensor(view3dSensor)
    agent.addDefaultEffectors()

    val flowerX = 280.0
    val flowerY = 190.0

    listOf(
        Triple(120, 120, EntityType.Swiss),
        Triple(430, 120, EntityType.Fish),
        Triple(120, 430, EntityType.Candle),
        Triple(430, 430, EntityType.Poison),
        Triple(flowerX.toInt(), flowerY.toInt(), EntityType.Flower),
        Triple(190, 320, EntityType.Tulip),
        Triple(360, 300, EntityType.Pansy),
        Triple(90, 270, EntityType.Lion),
    ).forEach { (x, y, type) ->
        odorWorld.addEntity(x, y, type).apply {
            if (type == EntityType.Swiss) {
                smellSource = SmellSource(doubleArrayOf(1.0, 0.0, 0.0, 0.0, 0.0, 0.0))
            }
        }
    }

    agent.heading = Math.toDegrees(atan2(flowerY - agent.y, flowerX - agent.x))
    view3dSensor.update(agent)
    agent.select()

    return View3dNavigationScene(odorWorldComponent, agent, view3dSensor)
}

fun neighborhoodAverage(
    values: DoubleArray,
    x: Int,
    y: Int,
    radius: Int,
    width: Int,
    height: Int
): Double {
    var sum = 0.0
    var weightSum = 0.0
    for (dy in -radius..radius) {
        for (dx in -radius..radius) {
            val nx = x + dx
            val ny = y + dy
            if (nx in 0 until width && ny in 0 until height) {
                val dist2 = (dx * dx) + (dy * dy)
                val weight = 1.0 / (1.0 + dist2)
                sum += values[(ny * width) + nx] * weight
                weightSum += weight
            }
        }
    }
    return if (weightSum > 0.0) sum / weightSum else 0.0
}

fun normalizeToUnitRange(values: DoubleArray): DoubleArray {
    if (values.isEmpty()) return values
    val min = values.minOrNull() ?: return values
    val max = values.maxOrNull() ?: return values
    val range = max - min
    if (range <= 1e-12) {
        return DoubleArray(values.size)
    }
    return DoubleArray(values.size) { i ->
        ((values[i] - min) / range).coerceIn(0.0, 1.0)
    }
}

fun simpleEdgeMap(input: DoubleArray, width: Int, height: Int): DoubleArray {
    val output = DoubleArray(input.size)
    for (y in 1 until (height - 1)) {
        for (x in 1 until (width - 1)) {
            val i = y * width + x
            val gx = input[i + 1] - input[i - 1]
            val gy = input[i + width] - input[i - width]
            output[i] = (kotlin.math.abs(gx) + kotlin.math.abs(gy)).coerceIn(0.0, 1.0)
        }
    }
    return output
}
