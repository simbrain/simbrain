package org.simbrain.world.odorworld.sensors

import org.simbrain.util.UserParameter
import org.simbrain.workspace.Producible
import org.simbrain.world.odorworld.Raycaster
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Color
import java.awt.image.BufferedImage

/**
 * A sensor that renders a pseudo-3D first-person view from the entity's perspective.
 * Uses raycasting to project the tilemap floor, world boundary walls, and entity sprites.
 *
 * The output is a raster array that can be coupled to neural networks or other components.
 */
class View3DSensor @JvmOverloads constructor(
    theta: Double = DEFAULT_THETA,
    radius: Double = DEFAULT_RADIUS
) : SensorWithRelativeLocation(theta, radius) {

    @UserParameter(
        label = "Field of View",
        description = "Horizontal field of view in degrees",
        order = 10
    )
    var fov: Double = 90.0

    @UserParameter(
        label = "View Distance",
        description = "Maximum render distance in pixels",
        order = 11
    )
    var viewDistance: Double = 500.0

    @UserParameter(
        label = "Width",
        description = "Output image width in pixels",
        order = 12
    )
    var outputWidth: Int = 64

    @UserParameter(
        label = "Height",
        description = "Output image height in pixels",
        order = 13
    )
    var outputHeight: Int = 64

    @UserParameter(
        label = "Horizon Position",
        description = "Where the horizon sits on screen (0.0 = bottom, 1.0 = top)",
        order = 14
    )
    var horizonPosition: Double = 0.5

    @UserParameter(
        label = "Camera Height",
        description = "Height of camera above ground in world units",
        order = 15
    )
    var cameraWorldHeight: Double = 16.0

    @UserParameter(
        label = "Wall Height",
        description = "Height of boundary walls relative to camera height (1.0 = horizon level, 2.0 = extends above horizon)",
        order = 16
    )
    var wallHeight: Double = 2.0

    @UserParameter(
        label = "Billboard Sprites",
        description = "When enabled, sprites always face the camera. When disabled, sprites face their entity's heading and appear foreshortened at angles.",
        order = 17
    )
    var billboardSprites: Boolean = true

    @UserParameter(
        label = "Sky Color",
        description = "Color of the sky/ceiling",
        order = 20
    )
    var skyColor: Color = Color(135, 206, 235)  // Sky blue

    @UserParameter(
        label = "Wall Color",
        description = "Base color of boundary walls (darker shade used for N/S walls)",
        order = 21
    )
    var wallColor: Color = Color(180, 140, 100)  // Sandy brown

    @Transient
    private var raycaster: Raycaster? = null

    /**
     * Back buffer for rendering. The raycaster writes to this.
     */
    @Transient
    private var backBuffer: BufferedImage? = null

    /**
     * Display buffer for visualization. This is swapped with backBuffer after rendering completes.
     * Using double buffering prevents flickering when the display reads while rendering is in progress.
     */
    @Transient
    private var displayBuffer: BufferedImage? = null

    @Transient
    private var _brightness: DoubleArray = DoubleArray(0)

    @Transient
    private var _red: DoubleArray = DoubleArray(0)

    @Transient
    private var _green: DoubleArray = DoubleArray(0)

    @Transient
    private var _blue: DoubleArray = DoubleArray(0)

    /**
     * Grayscale brightness values for each pixel (width * height).
     * Values are normalized to 0.0-1.0.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription")
    val brightness: DoubleArray
        get() = _brightness

    /**
     * Red channel values for each pixel (width * height).
     * Values are normalized to 0.0-1.0.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription", defaultVisibility = false)
    val red: DoubleArray
        get() = _red

    /**
     * Green channel values for each pixel (width * height).
     * Values are normalized to 0.0-1.0.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription", defaultVisibility = false)
    val green: DoubleArray
        get() = _green

    /**
     * Blue channel values for each pixel (width * height).
     * Values are normalized to 0.0-1.0.
     */
    @get:Producible(customDescriptionMethod = "getAttributeDescription", defaultVisibility = false)
    val blue: DoubleArray
        get() = _blue

    /**
     * The rendered image for visualization purposes.
     * Returns the display buffer (not the back buffer being rendered to) to prevent flickering.
     */
    val renderedImage: BufferedImage?
        get() = displayBuffer

    override fun update(parent: OdorWorldEntity) {
        ensureBuffersInitialized()

        val cameraPos = computeAbsoluteLocation(parent)

        // Render to back buffer
        raycaster?.render(
            world = parent.world,
            parentEntity = parent,
            cameraX = cameraPos.x,
            cameraY = cameraPos.y,
            cameraHeading = parent.heading,
            horizonPosition = horizonPosition,
            cameraWorldHeight = cameraWorldHeight,
            fov = fov,
            viewDistance = viewDistance,
            wallHeight = wallHeight,
            billboardSprites = billboardSprites,
            skyColor = skyColor,
            wallColor = wallColor,
            outputBuffer = backBuffer!!
        )

        // Swap buffers - display buffer now has the completed frame
        val temp = displayBuffer
        displayBuffer = backBuffer
        backBuffer = temp

        extractPixelArrays()
    }

    private fun ensureBuffersInitialized() {
        if (backBuffer == null || backBuffer!!.width != outputWidth || backBuffer!!.height != outputHeight) {
            backBuffer = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB)
            displayBuffer = BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_ARGB)
            raycaster = Raycaster(outputWidth, outputHeight)
            val size = outputWidth * outputHeight
            _brightness = DoubleArray(size)
            _red = DoubleArray(size)
            _green = DoubleArray(size)
            _blue = DoubleArray(size)
        }
    }

    private fun extractPixelArrays() {
        val pixels = displayBuffer!!.getRGB(0, 0, outputWidth, outputHeight, null, 0, outputWidth)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = ((color ushr 16) and 0xFF) / 255.0
            val g = ((color ushr 8) and 0xFF) / 255.0
            val b = (color and 0xFF) / 255.0
            _red[i] = r
            _green[i] = g
            _blue[i] = b
            // Standard luminance formula
            _brightness[i] = r * 0.2126 + g * 0.7152 + b * 0.0722
        }
    }

    override fun copy(): View3DSensor {
        return View3DSensor(theta, radius).applyCommonCopy().apply {
            fov = this@View3DSensor.fov
            viewDistance = this@View3DSensor.viewDistance
            outputWidth = this@View3DSensor.outputWidth
            outputHeight = this@View3DSensor.outputHeight
            horizonPosition = this@View3DSensor.horizonPosition
            cameraWorldHeight = this@View3DSensor.cameraWorldHeight
            wallHeight = this@View3DSensor.wallHeight
            billboardSprites = this@View3DSensor.billboardSprites
            skyColor = this@View3DSensor.skyColor
            wallColor = this@View3DSensor.wallColor
        }
    }

    override val name: String
        get() = "3D View Sensor"

    override var label: String = "3D View"

}
