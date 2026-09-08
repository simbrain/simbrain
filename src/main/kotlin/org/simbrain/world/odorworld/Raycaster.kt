/**
 * Software raycaster behind [org.simbrain.world.odorworld.sensors.View3DSensor]. Renders a first-person view of an
 * [OdorWorld]: floor-cast tile layers, maze walls found by walking the grid cell by cell, the world boundary when it
 * does not wrap, and entity sprites occluded through a per-column depth buffer. Walls carry a procedural brick texture
 * keyed on the distance along the wall, with a darker seam at every cell boundary, so depth and structure stay
 * readable at the low resolutions this sensor is normally coupled at.
 */
package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Where a ray first meets a wall. [distance] is the perpendicular depth along the camera direction (the ray
 * parameter for an unnormalized ray of the form direction plus plane offset), so it compares directly with sprite
 * depth. [side] is 0 for walls that face east or west and 1 for walls that face north or south. [u] is the hit
 * position along the wall's own cell edge in 0 until 1.
 */
data class WallHit(val distance: Double, val side: Int, val u: Double)

class Raycaster(private val screenWidth: Int, private val screenHeight: Int) {

    private val zBuffer = DoubleArray(screenWidth) { Double.MAX_VALUE }

    /**
     * @param cameraHeading Camera heading in degrees (0 = East, CCW positive)
     * @param horizonPosition Where horizon sits on screen (0.0 = bottom, 1.0 = top)
     * @param cameraWorldHeight Camera height above ground in world units
     * @param viewDistance Maximum render distance in world units
     * @param wallHeight Wall height relative to camera height (1.0 reaches the horizon)
     * @param billboardSprites If true, sprites always face camera. If false, sprites face entity heading.
     * @param textureWalls If true, walls get a brick texture and cell seams; otherwise they are flat shaded.
     */
    fun render(
        world: OdorWorld,
        parentEntity: OdorWorldEntity,
        cameraX: Double,
        cameraY: Double,
        cameraHeading: Double,
        horizonPosition: Double,
        cameraWorldHeight: Double,
        fov: Double,
        viewDistance: Double,
        wallHeight: Double,
        billboardSprites: Boolean,
        skyColor: Color,
        wallColor: Color,
        outputBuffer: BufferedImage,
        textureWalls: Boolean = true
    ) {
        zBuffer.fill(Double.MAX_VALUE)

        // OdorWorld headings are counterclockwise with y growing downward on screen, so sin is negated
        val dirRad = Math.toRadians(cameraHeading)
        val dirX = cos(dirRad)
        val dirY = -sin(dirRad)

        val planeScale = tan(Math.toRadians(fov / 2))
        val planeX = -dirY * planeScale
        val planeY = dirX * planeScale

        val horizon = (screenHeight * horizonPosition).toInt()
        val projectionConstant = screenHeight * cameraWorldHeight

        renderCeiling(outputBuffer, horizon, horizonPosition, viewDistance, skyColor)
        renderFloor(
            outputBuffer, world, cameraX, cameraY, dirX, dirY, planeX, planeY,
            horizon, projectionConstant, viewDistance, skyColor
        )

        val walls = WallStyle(wallColor, textureWalls, world.gridCellPixelSize, wallHeight * cameraWorldHeight)
        val maze = world.maze
        for (x in 0 until screenWidth) {
            val cameraXNorm = 2.0 * x / screenWidth - 1.0
            val rayDirX = dirX + planeX * cameraXNorm
            val rayDirY = dirY + planeY * cameraXNorm

            val hit = maze?.let { castMazeRay(it, world.gridCellPixelSize, cameraX, cameraY, rayDirX, rayDirY, viewDistance) }
                ?: if (world.wrapAround) null else castRayToBoundary(cameraX, cameraY, rayDirX, rayDirY, world.width, world.height, world.gridCellPixelSize)
            if (hit != null && hit.distance < viewDistance && hit.distance < zBuffer[x]) {
                zBuffer[x] = hit.distance
                drawWallColumn(outputBuffer, x, hit, horizon, projectionConstant, wallHeight, viewDistance, walls)
            }
        }

        renderSprites(
            outputBuffer, world, parentEntity, cameraX, cameraY, dirX, dirY, planeX, planeY,
            horizon, cameraWorldHeight, viewDistance, billboardSprites
        )
    }

    private fun renderCeiling(
        buffer: BufferedImage,
        horizon: Int,
        horizonPosition: Double,
        viewDistance: Double,
        skyColor: Color
    ) {
        for (y in 0 until horizon) {
            val rowDistance = (screenHeight * (1.0 - horizonPosition)) / (horizon - y + 0.5)
            val fogFactor = (1.0 - rowDistance / viewDistance).coerceIn(0.2, 1.0)
            val foggedSkyColor = applyFog(skyColor.rgb, fogFactor)
            for (x in 0 until screenWidth) {
                buffer.setRGB(x, y, foggedSkyColor)
            }
        }
    }

    private fun renderFloor(
        buffer: BufferedImage,
        world: OdorWorld,
        cameraX: Double,
        cameraY: Double,
        dirX: Double,
        dirY: Double,
        planeX: Double,
        planeY: Double,
        horizon: Int,
        projectionConstant: Double,
        viewDistance: Double,
        skyColor: Color
    ) {
        val tileMap = world.tileMap
        val tileWidth = tileMap.tileWidth
        val tileHeight = tileMap.tileHeight

        for (y in horizon until screenHeight) {
            val p = y - horizon + 0.5
            val rowDistance = projectionConstant / p

            if (rowDistance > viewDistance) {
                val fogFactor = (viewDistance / rowDistance).coerceIn(0.0, 1.0)
                val fadedSkyColor = applyFog(skyColor.rgb, 0.3 + 0.7 * (1.0 - fogFactor))
                for (x in 0 until screenWidth) {
                    buffer.setRGB(x, y, fadedSkyColor)
                }
                continue
            }

            // Rays at the left and right screen edges are dir minus plane and dir plus plane
            val rayDirX0 = dirX - planeX
            val rayDirY0 = dirY - planeY
            val rayDirX1 = dirX + planeX
            val rayDirY1 = dirY + planeY

            val floorStepX = rowDistance * (rayDirX1 - rayDirX0) / screenWidth
            val floorStepY = rowDistance * (rayDirY1 - rayDirY0) / screenWidth

            var floorX = cameraX + rowDistance * rayDirX0
            var floorY = cameraY + rowDistance * rayDirY0

            val fogFactor = fogFactor(rowDistance, viewDistance)

            for (x in 0 until screenWidth) {
                var worldFloorX = floorX
                var worldFloorY = floorY

                if (world.wrapAround) {
                    worldFloorX = ((worldFloorX % world.width) + world.width) % world.width
                    worldFloorY = ((worldFloorY % world.height) + world.height) % world.height
                }

                val tileX = (worldFloorX / tileWidth).toInt()
                val tileY = (worldFloorY / tileHeight).toInt()

                if (tileX >= 0 && tileX < tileMap.width && tileY >= 0 && tileY < tileMap.height) {
                    var finalColor: Int? = null
                    for (layer in tileMap.layers) {
                        if (!layer.visible) continue
                        val gid = layer[tileX, tileY]
                        if (gid > 0) {
                            val tileImage = tileMap.tileImage(gid)
                            if (tileImage is BufferedImage) {
                                val tx = ((worldFloorX % tileWidth).toInt() + tileWidth) % tileWidth
                                val ty = ((worldFloorY % tileHeight).toInt() + tileHeight) % tileHeight
                                val texX = tx.coerceIn(0, tileWidth - 1)
                                val texY = ty.coerceIn(0, tileHeight - 1)
                                val color = tileImage.getRGB(texX, texY)
                                val alpha = (color ushr 24) and 0xFF
                                if (alpha > 200) {
                                    finalColor = color
                                } else if (alpha > 0 && finalColor != null) {
                                    finalColor = blendColors(finalColor, color, alpha / 255.0)
                                } else if (alpha > 0) {
                                    finalColor = color
                                }
                            }
                        }
                    }
                    if (finalColor != null) {
                        buffer.setRGB(x, y, applyFog(finalColor, fogFactor))
                    } else {
                        buffer.setRGB(x, y, applyFog(Color(64, 64, 64).rgb, fogFactor))
                    }
                } else {
                    buffer.setRGB(x, y, applyFog(Color(32, 32, 32).rgb, fogFactor))
                }

                floorX += floorStepX
                floorY += floorStepY
            }
        }
    }

    private class WallStyle(
        val color: Color,
        val textured: Boolean,
        val cellSize: Double,
        val wallWorldHeight: Double
    ) {
        val darkColor: Color = color.darker()
    }

    /**
     * Paints one screen column of wall for [hit], from the floor line up to the wall's projected height.
     */
    private fun drawWallColumn(
        buffer: BufferedImage,
        x: Int,
        hit: WallHit,
        horizon: Int,
        projectionConstant: Double,
        wallHeight: Double,
        viewDistance: Double,
        style: WallStyle
    ) {
        val floorScreenY = horizon + projectionConstant / hit.distance
        val wallScreenHeight = (wallHeight * projectionConstant) / hit.distance
        val drawStart = (floorScreenY - wallScreenHeight).toInt().coerceIn(0, screenHeight - 1)
        val drawEnd = floorScreenY.toInt().coerceIn(0, screenHeight - 1)

        val baseColor = (if (hit.side == 0) style.color else style.darkColor).rgb
        val fog = fogFactor(hit.distance, viewDistance)
        val seamShade = if (style.textured) seamShade(hit.u) else 1.0

        for (y in drawStart..drawEnd) {
            val shade = if (style.textured) {
                val v = ((floorScreenY - y) / wallScreenHeight).coerceIn(0.0, 1.0)
                seamShade * brickShade(hit.u, v, style.cellSize, style.wallWorldHeight)
            } else {
                1.0
            }
            buffer.setRGB(x, y, applyFog(baseColor, fog * shade))
        }
    }

    /**
     * Walks the grid cell by cell from the camera along the ray and returns the first closed edge of [maze] it
     * crosses within [maxDistance], or null. The ray direction is the camera direction plus a plane offset, so the
     * returned distance is perpendicular depth rather than Euclidean distance.
     */
    fun castMazeRay(
        maze: Maze,
        cellSize: Double,
        startX: Double,
        startY: Double,
        rayDirX: Double,
        rayDirY: Double,
        maxDistance: Double
    ): WallHit? {
        var column = floor(startX / cellSize).toInt()
        var row = floor(startY / cellSize).toInt()

        val stepX = if (rayDirX > 1e-9) 1 else if (rayDirX < -1e-9) -1 else 0
        val stepY = if (rayDirY > 1e-9) 1 else if (rayDirY < -1e-9) -1 else 0
        if (stepX == 0 && stepY == 0) return null

        val deltaX = if (stepX == 0) Double.MAX_VALUE else cellSize / abs(rayDirX)
        val deltaY = if (stepY == 0) Double.MAX_VALUE else cellSize / abs(rayDirY)
        var nextX = when (stepX) {
            1 -> ((column + 1) * cellSize - startX) / rayDirX
            -1 -> (column * cellSize - startX) / rayDirX
            else -> Double.MAX_VALUE
        }
        var nextY = when (stepY) {
            1 -> ((row + 1) * cellSize - startY) / rayDirY
            -1 -> (row * cellSize - startY) / rayDirY
            else -> Double.MAX_VALUE
        }

        // each crossing advances t by a positive delta, so the distance check alone ends the walk
        while (true) {
            if (nextX < nextY) {
                val t = nextX
                if (t > maxDistance) return null
                val direction = if (stepX > 0) GridDirection.EAST else GridDirection.WEST
                if (maze.hasEdgeWall(column, row, direction)) {
                    val hitY = startY + t * rayDirY
                    return WallHit(t, 0, fraction(hitY / cellSize))
                }
                column += stepX
                nextX += deltaX
            } else {
                val t = nextY
                if (t > maxDistance) return null
                val direction = if (stepY > 0) GridDirection.SOUTH else GridDirection.NORTH
                if (maze.hasEdgeWall(column, row, direction)) {
                    val hitX = startX + t * rayDirX
                    return WallHit(t, 1, fraction(hitX / cellSize))
                }
                row += stepY
                nextY += deltaY
            }
        }
    }

    private fun renderSprites(
        buffer: BufferedImage,
        world: OdorWorld,
        parentEntity: OdorWorldEntity,
        cameraX: Double,
        cameraY: Double,
        dirX: Double,
        dirY: Double,
        planeX: Double,
        planeY: Double,
        horizon: Int,
        cameraWorldHeight: Double,
        viewDistance: Double,
        billboardSprites: Boolean
    ) {
        data class SpriteData(
            val entity: OdorWorldEntity,
            val distance: Double,
            val relX: Double,
            val relY: Double
        )

        val sprites = world.entityList
            .filter { it != parentEntity }
            .map { entity ->
                var dx = entity.x - cameraX
                var dy = entity.y - cameraY
                if (world.wrapAround) {
                    if (dx > world.width / 2) dx -= world.width
                    else if (dx < -world.width / 2) dx += world.width
                    if (dy > world.height / 2) dy -= world.height
                    else if (dy < -world.height / 2) dy += world.height
                }
                SpriteData(entity, sqrt(dx * dx + dy * dy), dx, dy)
            }
            .filter { it.distance < viewDistance && it.distance > 0.1 }
            .sortedByDescending { it.distance }

        for (sprite in sprites) {
            drawSprite(
                buffer, sprite.entity, sprite.relX, sprite.relY,
                dirX, dirY, planeX, planeY, horizon, cameraWorldHeight, billboardSprites
            )
        }
    }

    private fun drawSprite(
        buffer: BufferedImage,
        entity: OdorWorldEntity,
        relX: Double,
        relY: Double,
        dirX: Double,
        dirY: Double,
        planeX: Double,
        planeY: Double,
        horizon: Int,
        cameraWorldHeight: Double,
        billboardSprites: Boolean
    ) {
        val invDet = 1.0 / (planeX * dirY - dirX * planeY)
        val transformX = invDet * (dirY * relX - dirX * relY)
        val transformY = invDet * (-planeY * relX + planeX * relY)

        if (transformY <= 0.1) return

        val spriteScreenX = ((screenWidth / 2.0) * (1 + transformX / transformY)).toInt()

        // relY is in screen coordinates, so negate it for the mathematical angle convention
        val viewingAngle = Math.toDegrees(atan2(-relY, relX))

        val spriteInfo = getEntityImage(entity, viewingAngle)
        val entityImage = spriteInfo.image
        val entityWorldWidth = entity.entityType.width.toDouble()
        val entityWorldHeight = entity.entityType.height.toDouble()

        val projectionConstant = screenHeight * cameraWorldHeight
        val floorScreenY = (horizon + projectionConstant / transformY).toInt()

        val spriteHeight = ((entityWorldHeight * screenHeight) / transformY).toInt()
            .coerceIn(1, screenHeight * 4)
        var spriteWidth = ((entityWorldWidth * screenHeight) / transformY).toInt()
            .coerceIn(1, screenWidth * 4)

        // Fixed-orientation sprites foreshorten by how far the view is from the sprite's own facing
        if (!billboardSprites) {
            val foreshortening = abs(cos(Math.toRadians(spriteInfo.angleDiffDegrees)))
            spriteWidth = (spriteWidth * foreshortening).toInt()
            if (spriteWidth < 1) return
        }

        val flipHorizontal = !billboardSprites && abs(spriteInfo.angleDiffDegrees) > 90.0

        val spriteTop = floorScreenY - spriteHeight / 2
        val spriteBottom = floorScreenY + spriteHeight / 2

        val drawStartX = (spriteScreenX - spriteWidth / 2).coerceIn(0, screenWidth)
        val drawEndX = (spriteScreenX + spriteWidth / 2).coerceIn(0, screenWidth)
        val drawStartY = spriteTop.coerceIn(0, screenHeight)
        val drawEndY = spriteBottom.coerceIn(0, screenHeight)

        if (drawStartX >= drawEndX || drawStartY >= drawEndY) return
        if (spriteWidth <= 0 || spriteHeight <= 0) return

        for (x in drawStartX until drawEndX) {
            if (transformY < zBuffer[x]) {
                val spriteColumnX = x - (spriteScreenX - spriteWidth / 2)
                var texX = (spriteColumnX * entityImage.width / spriteWidth)
                    .coerceIn(0, entityImage.width - 1)
                if (flipHorizontal) {
                    texX = entityImage.width - 1 - texX
                }
                for (y in drawStartY until drawEndY) {
                    val spriteRowY = y - spriteTop
                    val texY = (spriteRowY * entityImage.height / spriteHeight)
                        .coerceIn(0, entityImage.height - 1)
                    val color = entityImage.getRGB(texX, texY)
                    val alpha = (color ushr 24) and 0xFF
                    if (alpha > 128) {
                        buffer.setRGB(x, y, color)
                    }
                }
            }
        }
    }

    /**
     * @param angleDiffDegrees Difference from the sprite's optimal viewing angle, -180 to 180. Small for
     * multi-direction sprites, up to a half turn for single-direction ones.
     */
    private data class SpriteInfo(
        val image: BufferedImage,
        val angleDiffDegrees: Double
    )

    private fun getEntityImage(entity: OdorWorldEntity, viewingAngle: Double): SpriteInfo {
        val directionIndex = entity.getDirectionIndex(viewingAngle)
        val angleDiffDegrees = entity.getAngleDiffFromOptimal(viewingAngle, directionIndex)
        val image = entity.getImage(directionIndex)
        return SpriteInfo(image, angleDiffDegrees)
    }

    /**
     * The nearest world boundary the ray meets, or null when it meets none. [WallHit.u] is the position along the
     * boundary within the current grid cell, so the boundary textures like a maze wall.
     */
    private fun castRayToBoundary(
        startX: Double,
        startY: Double,
        dirX: Double,
        dirY: Double,
        worldWidth: Double,
        worldHeight: Double,
        cellSize: Double
    ): WallHit? {
        var best: WallHit? = null

        fun consider(t: Double, side: Int, along: Double) {
            if (t > 0 && (best == null || t < best!!.distance)) {
                best = WallHit(t, side, fraction(along / cellSize))
            }
        }

        if (dirX < -0.0001) {
            val t = -startX / dirX
            val hitY = startY + t * dirY
            if (hitY >= 0 && hitY <= worldHeight) consider(t, 0, hitY)
        }
        if (dirX > 0.0001) {
            val t = (worldWidth - startX) / dirX
            val hitY = startY + t * dirY
            if (hitY >= 0 && hitY <= worldHeight) consider(t, 0, hitY)
        }
        if (dirY < -0.0001) {
            val t = -startY / dirY
            val hitX = startX + t * dirX
            if (hitX >= 0 && hitX <= worldWidth) consider(t, 1, hitX)
        }
        if (dirY > 0.0001) {
            val t = (worldHeight - startY) / dirY
            val hitX = startX + t * dirX
            if (hitX >= 0 && hitX <= worldWidth) consider(t, 1, hitX)
        }
        return best
    }

    /**
     * Fog darkens quadratically with depth so nearby walls stay bright and far ones fall off quickly, which makes
     * the distance to a wall easier to read than a linear ramp does.
     */
    private fun fogFactor(distance: Double, viewDistance: Double): Double {
        val remaining = 1.0 - (distance / viewDistance).coerceIn(0.0, 1.0)
        return (remaining * remaining).coerceAtLeast(0.12)
    }

    /**
     * Darkening for the vertical seam where one cell edge meets the next.
     */
    private fun seamShade(u: Double): Double {
        val toEdge = min(u, 1.0 - u)
        return if (toEdge < SEAM_WIDTH) 0.55 else 1.0
    }

    /**
     * Brick pattern: four bricks per cell width, rows [BRICK_ROWS_PER_CELL_HEIGHT] scaled by the wall's world
     * height so bricks stay roughly square, alternate rows offset by half a brick, mortar drawn darker.
     */
    private fun brickShade(u: Double, v: Double, cellSize: Double, wallWorldHeight: Double): Double {
        val rowsPerWall = max(1.0, BRICK_ROWS_PER_CELL_HEIGHT * wallWorldHeight / cellSize)
        val rowPosition = v * rowsPerWall
        val rowIndex = floor(rowPosition).toInt()
        val withinRow = rowPosition - rowIndex
        val offset = if (rowIndex % 2 == 0) 0.0 else 0.5
        val withinBrick = fraction(u * BRICKS_PER_CELL + offset)
        return if (withinRow < MORTAR_FRACTION || withinBrick < MORTAR_FRACTION) 0.72 else 1.0
    }

    private fun fraction(value: Double): Double {
        val f = value - floor(value)
        return if (f < 0) f + 1.0 else f
    }

    private fun applyFog(color: Int, fogFactor: Double): Int {
        val a = (color ushr 24) and 0xFF
        val r = (((color ushr 16) and 0xFF) * fogFactor).toInt().coerceIn(0, 255)
        val g = (((color ushr 8) and 0xFF) * fogFactor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * fogFactor).toInt().coerceIn(0, 255)
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun blendColors(background: Int, foreground: Int, alpha: Double): Int {
        val bgR = (background ushr 16) and 0xFF
        val bgG = (background ushr 8) and 0xFF
        val bgB = background and 0xFF
        val fgR = (foreground ushr 16) and 0xFF
        val fgG = (foreground ushr 8) and 0xFF
        val fgB = foreground and 0xFF
        val r = (fgR * alpha + bgR * (1 - alpha)).toInt()
        val g = (fgG * alpha + bgG * (1 - alpha)).toInt()
        val b = (fgB * alpha + bgB * (1 - alpha)).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {
        private const val SEAM_WIDTH = 0.035
        private const val BRICKS_PER_CELL = 4.0
        private const val BRICK_ROWS_PER_CELL_HEIGHT = 8.0
        private const val MORTAR_FRACTION = 0.12
    }
}
