package org.simbrain.world.odorworld

import org.simbrain.world.odorworld.entities.OdorWorldEntity
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.*

/**
 * Raycasting engine for rendering pseudo-3D views of OdorWorld.
 *
 * Uses classic floor-casting and sprite rendering techniques to create
 * a first-person perspective view.
 */
class Raycaster(private val screenWidth: Int, private val screenHeight: Int) {

    // Z-buffer for sprite occlusion (one entry per screen column)
    private val zBuffer = DoubleArray(screenWidth) { Double.MAX_VALUE }

    /**
     * Render the 3D view to the output buffer.
     *
     * @param world The OdorWorld to render
     * @param parentEntity The entity that owns this sensor (excluded from rendering)
     * @param cameraX Camera X position in world coordinates
     * @param cameraY Camera Y position in world coordinates
     * @param cameraHeading Camera heading in degrees (0 = East, CCW positive)
     * @param horizonPosition Where horizon sits on screen (0.0 = bottom, 1.0 = top)
     * @param cameraWorldHeight Camera height above ground in world units
     * @param fov Field of view in degrees
     * @param viewDistance Maximum render distance
     * @param wallHeight Height of boundary walls
     * @param billboardSprites If true, sprites always face camera. If false, sprites face entity heading.
     * @param skyColor Color of the sky/ceiling
     * @param wallColor Base color for boundary walls
     * @param outputBuffer BufferedImage to render into
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
        outputBuffer: BufferedImage
    ) {
        // Clear z-buffer
        zBuffer.fill(Double.MAX_VALUE)

        // Convert heading to radians (OdorWorld: 0=East, CCW positive)
        // In screen coordinates, Y increases downward, so we negate sin
        val dirRad = Math.toRadians(cameraHeading)
        val dirX = cos(dirRad)
        val dirY = -sin(dirRad)

        // Camera plane perpendicular to direction (for FOV)
        val planeScale = tan(Math.toRadians(fov / 2))
        val planeX = -dirY * planeScale
        val planeY = dirX * planeScale

        // 1. Render ceiling (simple gradient)
        renderCeiling(outputBuffer, horizonPosition, viewDistance, skyColor)

        // 2. Render floor with tilemap textures
        renderFloor(
            outputBuffer, world,
            cameraX, cameraY,
            dirX, dirY,
            planeX, planeY,
            horizonPosition, cameraWorldHeight, viewDistance,
            skyColor
        )

        // 3. Render boundary walls (if wrapAround is false)
        if (!world.wrapAround) {
            renderBoundaryWalls(
                outputBuffer, world,
                cameraX, cameraY,
                dirX, dirY,
                planeX, planeY,
                horizonPosition, cameraWorldHeight, wallHeight, viewDistance,
                wallColor
            )
        }

        // 4. Render entity sprites
        renderSprites(
            outputBuffer, world, parentEntity,
            cameraX, cameraY,
            dirX, dirY,
            planeX, planeY,
            horizonPosition, cameraWorldHeight, viewDistance,
            billboardSprites
        )
    }

    private fun renderCeiling(
        buffer: BufferedImage,
        horizonPosition: Double,
        viewDistance: Double,
        skyColor: Color
    ) {
        val horizon = (screenHeight * horizonPosition).toInt()

        for (y in 0 until horizon) {
            // Distance-based fog for ceiling
            val rowDistance = (screenHeight * (1.0 - horizonPosition)) / (horizon - y + 0.5)
            val fogFactor = (1.0 - rowDistance / viewDistance).coerceIn(0.2, 1.0)

            // Sky color with fog
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
        horizonPosition: Double,
        cameraWorldHeight: Double,
        viewDistance: Double,
        skyColor: Color
    ) {
        val tileMap = world.tileMap
        val tileWidth = tileMap.tileWidth
        val tileHeight = tileMap.tileHeight
        val horizon = (screenHeight * horizonPosition).toInt()

        // Projection constant based on camera world height
        val projectionConstant = screenHeight * cameraWorldHeight

        // Floor casting - iterate from horizon to bottom
        for (y in horizon until screenHeight) {
            // Current row distance from camera (perspective projection)
            val p = y - horizon + 0.5
            val rowDistance = projectionConstant / p

            if (rowDistance > viewDistance) {
                // Beyond view distance - blend into sky color
                val fogFactor = (viewDistance / rowDistance).coerceIn(0.0, 1.0)
                val fadedSkyColor = applyFog(skyColor.rgb, 0.3 + 0.7 * (1.0 - fogFactor))
                for (x in 0 until screenWidth) {
                    buffer.setRGB(x, y, fadedSkyColor)
                }
                continue
            }

            // Calculate the real world step vector for this row
            // At the left edge of screen (x=0), ray is dir - plane
            // At the right edge (x=screenWidth), ray is dir + plane
            val rayDirX0 = dirX - planeX
            val rayDirY0 = dirY - planeY
            val rayDirX1 = dirX + planeX
            val rayDirY1 = dirY + planeY

            // Step vectors for moving across the row
            val floorStepX = rowDistance * (rayDirX1 - rayDirX0) / screenWidth
            val floorStepY = rowDistance * (rayDirY1 - rayDirY0) / screenWidth

            // Starting floor position at left edge
            var floorX = cameraX + rowDistance * rayDirX0
            var floorY = cameraY + rowDistance * rayDirY0

            val fogFactor = (1.0 - rowDistance / viewDistance).coerceIn(0.3, 1.0)

            for (x in 0 until screenWidth) {
                // Wrap coordinates for wrap-around worlds
                var worldFloorX = floorX
                var worldFloorY = floorY

                if (world.wrapAround) {
                    worldFloorX = ((worldFloorX % world.width) + world.width) % world.width
                    worldFloorY = ((worldFloorY % world.height) + world.height) % world.height
                }

                // Get tile coordinates
                val tileX = (worldFloorX / tileWidth).toInt()
                val tileY = (worldFloorY / tileHeight).toInt()

                // Check if within tilemap bounds
                if (tileX >= 0 && tileX < tileMap.width && tileY >= 0 && tileY < tileMap.height) {
                    // Get the topmost non-zero tile from all layers (composite)
                    var finalColor: Int? = null

                    // Iterate through layers from bottom to top
                    for (layer in tileMap.layers) {
                        val gid = layer[tileX, tileY]
                        if (gid > 0) {
                            val tileImage = tileMap.tileImage(gid)
                            if (tileImage is BufferedImage) {
                                // UV coordinates within tile
                                val tx = ((worldFloorX % tileWidth).toInt() + tileWidth) % tileWidth
                                val ty = ((worldFloorY % tileHeight).toInt() + tileHeight) % tileHeight

                                val texX = tx.coerceIn(0, tileWidth - 1)
                                val texY = ty.coerceIn(0, tileHeight - 1)

                                val color = tileImage.getRGB(texX, texY)
                                val alpha = (color ushr 24) and 0xFF

                                // Blend with previous layer if semi-transparent
                                if (alpha > 200) {
                                    finalColor = color
                                } else if (alpha > 0 && finalColor != null) {
                                    // Simple alpha blend
                                    finalColor = blendColors(finalColor, color, alpha / 255.0)
                                } else if (alpha > 0) {
                                    finalColor = color
                                }
                            }
                        }
                    }

                    if (finalColor != null) {
                        val foggedColor = applyFog(finalColor, fogFactor)
                        buffer.setRGB(x, y, foggedColor)
                    } else {
                        // Empty tile - render as dark floor
                        buffer.setRGB(x, y, applyFog(Color(64, 64, 64).rgb, fogFactor))
                    }
                } else {
                    // Outside tilemap bounds - render as void
                    buffer.setRGB(x, y, applyFog(Color(32, 32, 32).rgb, fogFactor))
                }

                floorX += floorStepX
                floorY += floorStepY
            }
        }
    }

    private fun renderBoundaryWalls(
        buffer: BufferedImage,
        world: OdorWorld,
        cameraX: Double,
        cameraY: Double,
        dirX: Double,
        dirY: Double,
        planeX: Double,
        planeY: Double,
        horizonPosition: Double,
        cameraWorldHeight: Double,
        wallHeight: Double,
        viewDistance: Double,
        wallColor: Color
    ) {
        val horizon = (screenHeight * horizonPosition).toInt()

        // Create darker shade for N/S walls (side shading)
        val wallColorDark = Color(
            (wallColor.red * 0.7).toInt().coerceIn(0, 255),
            (wallColor.green * 0.7).toInt().coerceIn(0, 255),
            (wallColor.blue * 0.7).toInt().coerceIn(0, 255)
        )

        // Cast a ray for each screen column
        for (x in 0 until screenWidth) {
            // Calculate ray direction for this column
            val cameraXNorm = 2.0 * x / screenWidth - 1.0
            val rayDirX = dirX + planeX * cameraXNorm
            val rayDirY = dirY + planeY * cameraXNorm

            // Find intersection with world boundaries
            val (hitDist, hitSide) = castRayToBoundary(
                cameraX, cameraY,
                rayDirX, rayDirY,
                world.width, world.height
            )

            if (hitDist < viewDistance && hitDist < zBuffer[x]) {
                zBuffer[x] = hitDist

                // Calculate where the floor at this distance appears on screen
                val projectionConstant = screenHeight * cameraWorldHeight
                val floorScreenY = (horizon + projectionConstant / hitDist).toInt()

                // Calculate wall height on screen (wall extends upward from floor)
                // wallHeight is relative to camera height: 1.0 means wall reaches horizon
                val wallScreenHeight = ((wallHeight * projectionConstant) / hitDist).toInt()
                val drawStart = (floorScreenY - wallScreenHeight).coerceIn(0, screenHeight - 1)
                val drawEnd = floorScreenY.coerceIn(0, screenHeight - 1)

                // Wall color with side shading (darker on N/S walls)
                val baseColor = if (hitSide == 0) wallColor else wallColorDark
                val fogFactor = (1.0 - hitDist / viewDistance).coerceIn(0.3, 1.0)
                val finalWallColor = applyFog(baseColor.rgb, fogFactor)

                for (y in drawStart..drawEnd) {
                    buffer.setRGB(x, y, finalWallColor)
                }
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
        horizonPosition: Double,
        cameraWorldHeight: Double,
        viewDistance: Double,
        billboardSprites: Boolean
    ) {
        val horizon = (screenHeight * horizonPosition).toInt()

        // Collect entities with their distances, excluding parent entity
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

                // Handle wrap-around distance calculation
                if (world.wrapAround) {
                    if (dx > world.width / 2) dx -= world.width
                    else if (dx < -world.width / 2) dx += world.width
                    if (dy > world.height / 2) dy -= world.height
                    else if (dy < -world.height / 2) dy += world.height
                }

                val dist = sqrt(dx * dx + dy * dy)
                SpriteData(entity, dist, dx, dy)
            }
            .filter { it.distance < viewDistance && it.distance > 0.1 }
            .sortedByDescending { it.distance }  // Back to front

        for (sprite in sprites) {
            drawSprite(
                buffer, sprite.entity,
                sprite.relX, sprite.relY,
                dirX, dirY, planeX, planeY,
                horizon, cameraWorldHeight,
                billboardSprites
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
        // Transform sprite position to camera space
        val invDet = 1.0 / (planeX * dirY - dirX * planeY)
        val transformX = invDet * (dirY * relX - dirX * relY)
        val transformY = invDet * (-planeY * relX + planeX * relY)

        // transformY is the depth (distance in camera direction)
        if (transformY <= 0.1) return  // Behind camera or too close

        // Calculate sprite screen X position
        val spriteScreenX = ((screenWidth / 2.0) * (1 + transformX / transformY)).toInt()

        // Calculate viewing angle from camera to entity (for sprite selection)
        // atan2 gives angle in radians, convert to degrees
        // Note: relY is in screen coords (positive = down), so negate for standard math coords
        val viewingAngle = Math.toDegrees(atan2(-relY, relX))

        // Get entity image and sprite metadata based on viewing angle
        val spriteInfo = getEntityImage(entity, viewingAngle)
        val entityImage = spriteInfo.image
        val entityWorldWidth = entity.entityType.width.toDouble()
        val entityWorldHeight = entity.entityType.height.toDouble()

        // Projection constant based on camera world height
        val projectionConstant = screenHeight * cameraWorldHeight

        // Calculate where the floor at this distance appears on screen
        val floorScreenY = (horizon + projectionConstant / transformY).toInt()

        // Calculate sprite size on screen based on entity's world size and distance
        val spriteHeight = ((entityWorldHeight * screenHeight) / transformY).toInt()
            .coerceIn(1, screenHeight * 4)
        var spriteWidth = ((entityWorldWidth * screenHeight) / transformY).toInt()
            .coerceIn(1, screenWidth * 4)

        // For fixed-orientation sprites (non-billboard), apply foreshortening
        // angleDiffDegrees is how far the viewing angle is from the sprite's optimal angle
        // For 8-direction sprites: max ±22.5° -> cos(22.5°) ≈ 0.92
        // For 24-direction sprites: max ±7.5° -> cos(7.5°) ≈ 0.99
        // For 1-direction sprites: up to ±180° -> full range of foreshortening
        // Use abs() because viewing from behind (180°) should also show full width (just flipped)
        if (!billboardSprites) {
            val foreshortening = abs(cos(Math.toRadians(spriteInfo.angleDiffDegrees)))

            // Apply foreshortening to width
            spriteWidth = (spriteWidth * foreshortening).toInt()

            // If sprite is too thin (nearly edge-on), skip rendering
            if (spriteWidth < 1) return
        }

        // Determine if we should flip the image (viewing from behind)
        // Only happens when angleDiff > 90°, which is only possible for sprites with few directions
        val flipHorizontal = !billboardSprites && abs(spriteInfo.angleDiffDegrees) > 90.0

        // Center the sprite at the floor projection point
        // This assumes images have their content centered (with equal padding on all sides)
        val spriteCenterY = floorScreenY
        val spriteTop = spriteCenterY - spriteHeight / 2
        val spriteBottom = spriteCenterY + spriteHeight / 2

        val drawStartX = (spriteScreenX - spriteWidth / 2).coerceIn(0, screenWidth)
        val drawEndX = (spriteScreenX + spriteWidth / 2).coerceIn(0, screenWidth)
        val drawStartY = spriteTop.coerceIn(0, screenHeight)
        val drawEndY = spriteBottom.coerceIn(0, screenHeight)

        // Early exit if sprite is completely off screen
        if (drawStartX >= drawEndX || drawStartY >= drawEndY) return
        if (spriteWidth <= 0 || spriteHeight <= 0) return

        // Draw the sprite column by column
        for (x in drawStartX until drawEndX) {
            // Check z-buffer - only draw if closer than wall
            if (transformY < zBuffer[x]) {
                // Calculate texture X coordinate
                val spriteColumnX = x - (spriteScreenX - spriteWidth / 2)
                var texX = (spriteColumnX * entityImage.width / spriteWidth)
                    .coerceIn(0, entityImage.width - 1)

                // Flip horizontally if viewing from behind
                if (flipHorizontal) {
                    texX = entityImage.width - 1 - texX
                }

                for (y in drawStartY until drawEndY) {
                    // Calculate texture Y coordinate
                    val spriteRowY = y - spriteTop
                    val texY = (spriteRowY * entityImage.height / spriteHeight)
                        .coerceIn(0, entityImage.height - 1)

                    val color = entityImage.getRGB(texX, texY)
                    val alpha = (color ushr 24) and 0xFF

                    // Only draw non-transparent pixels
                    if (alpha > 128) {
                        buffer.setRGB(x, y, color)
                    }
                }
            }
        }
    }

    /**
     * Data class holding sprite rendering information.
     *
     * @param image The sprite image to render
     * @param angleDiffDegrees The angle difference from optimal viewing angle (for foreshortening).
     *                         Range is -180 to 180. For multi-direction sprites this is small (e.g., ±22.5° for 8 dirs).
     *                         For single-direction sprites this can be up to ±180°.
     */
    private data class SpriteInfo(
        val image: BufferedImage,
        val angleDiffDegrees: Double
    )

    /**
     * Get the appropriate sprite image for an entity based on viewing angle.
     * Uses shared utilities from OdorWorldUtils for image retrieval and caching.
     *
     * @param entity The entity to get the image for
     * @param viewingAngle The angle (in degrees) from camera to entity (0 = East, CCW positive)
     * @return SpriteInfo containing the image and angle difference from optimal viewing
     */
    private fun getEntityImage(entity: OdorWorldEntity, viewingAngle: Double): SpriteInfo {
        val directionIndex = entity.getDirectionIndex(viewingAngle)
        val angleDiffDegrees = entity.getAngleDiffFromOptimal(viewingAngle, directionIndex)
        val image = entity.getImage(directionIndex)

        return SpriteInfo(image, angleDiffDegrees)
    }

    /**
     * Cast a ray to find the nearest boundary wall intersection.
     *
     * @return Pair of (distance, side) where side is 0 for E/W walls, 1 for N/S walls
     */
    private fun castRayToBoundary(
        startX: Double,
        startY: Double,
        dirX: Double,
        dirY: Double,
        worldWidth: Double,
        worldHeight: Double
    ): Pair<Double, Int> {
        var minDist = Double.MAX_VALUE
        var hitSide = 0

        // Check intersection with each boundary

        // Left boundary (x = 0)
        if (dirX < -0.0001) {
            val t = -startX / dirX
            if (t > 0 && t < minDist) {
                val hitY = startY + t * dirY
                if (hitY >= 0 && hitY <= worldHeight) {
                    minDist = t
                    hitSide = 0
                }
            }
        }

        // Right boundary (x = worldWidth)
        if (dirX > 0.0001) {
            val t = (worldWidth - startX) / dirX
            if (t > 0 && t < minDist) {
                val hitY = startY + t * dirY
                if (hitY >= 0 && hitY <= worldHeight) {
                    minDist = t
                    hitSide = 0
                }
            }
        }

        // Top boundary (y = 0)
        if (dirY < -0.0001) {
            val t = -startY / dirY
            if (t > 0 && t < minDist) {
                val hitX = startX + t * dirX
                if (hitX >= 0 && hitX <= worldWidth) {
                    minDist = t
                    hitSide = 1
                }
            }
        }

        // Bottom boundary (y = worldHeight)
        if (dirY > 0.0001) {
            val t = (worldHeight - startY) / dirY
            if (t > 0 && t < minDist) {
                val hitX = startX + t * dirX
                if (hitX >= 0 && hitX <= worldWidth) {
                    minDist = t
                    hitSide = 1
                }
            }
        }

        return Pair(minDist, hitSide)
    }

    /**
     * Apply distance fog to a color.
     */
    private fun applyFog(color: Int, fogFactor: Double): Int {
        val a = (color ushr 24) and 0xFF
        val r = (((color ushr 16) and 0xFF) * fogFactor).toInt()
        val g = (((color ushr 8) and 0xFF) * fogFactor).toInt()
        val b = ((color and 0xFF) * fogFactor).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    /**
     * Blend two colors together using alpha blending.
     */
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
}