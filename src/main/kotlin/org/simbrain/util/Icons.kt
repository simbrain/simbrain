package org.simbrain.util

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.Image
import java.awt.RenderingHints
import java.awt.image.BaseMultiResolutionImage
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import javax.swing.ImageIcon

/**
 * Central icon resolution for Simbrain's Swing UI.
 *
 * Given a legacy raster path (e.g. `"menu_icons/Save.png"`), [small]/[sized] return a crisp,
 * theme-aware icon: a scalable [FlatSVGIcon] when a matching SVG exists at `icons/<kebab>.svg`
 * (or `icons/multicolor/<kebab>.svg`), else a HiDPI-correct downscale of the raster as a
 * fallback. The raster base name is kebab-cased to find the SVG, so `Save.png` -> `icons/save.svg`,
 * `ZoomIn.png` -> `icons/zoom-in.svg`, `brokenChainIcon.png` -> `icons/broken-chain-icon.svg`.
 *
 * Because [FlatSVGIcon] extends [ImageIcon] and the raster fallback is also an [ImageIcon], the
 * return type stays [ImageIcon] and existing callers keep working unchanged — including those
 * that call [ImageIcon.getImage]. Dropping an SVG into `icons/` auto-upgrades that icon with no
 * code change.
 *
 * Single-color SVGs are recolored to the LaF foreground by the global [FlatSVGIcon.ColorFilter]
 * installed in [installSimbrainSvgIconColors]; multicolor icons live in `icons/multicolor/` and
 * avoid pure black so the filter passes them through.
 */
object Icons {

    /** Canonical menu/toolbar/button icon size, in logical pixels (FlatLaf standard). */
    const val SMALL = 16

    private val svgNameCache = ConcurrentHashMap<String, String>()
    private val rasterCache = ConcurrentHashMap<String, ImageIcon>()

    @JvmStatic
    fun small(rasterPath: String): ImageIcon? = sized(rasterPath, SMALL)

    @JvmStatic
    @JvmOverloads
    fun sized(rasterPath: String, size: Int = SMALL): ImageIcon? {
        val svg = svgNameFor(rasterPath)
        if (svg != null) return FlatSVGIcon(svg, size, size)
        return rasterFallback(rasterPath, size)
    }

    private fun svgNameFor(rasterPath: String): String? {
        svgNameCache[rasterPath]?.let { return it.ifEmpty { null } }
        val resolved = resolveSvg(rasterPath)
        svgNameCache[rasterPath] = resolved ?: ""
        return resolved
    }

    private fun resolveSvg(rasterPath: String): String? {
        val base = rasterPath.substringAfterLast('/').substringBeforeLast('.')
        val name = kebab(base)
        for (dir in listOf("icons", "icons/multicolor")) {
            val candidate = "$dir/$name.svg"
            if (ClassLoader.getSystemClassLoader().getResource(candidate) != null) return candidate
        }
        return null
    }

    private fun rasterFallback(rasterPath: String, size: Int): ImageIcon? {
        val key = "$rasterPath@$size"
        rasterCache[key]?.let { return it }
        val url = ClassLoader.getSystemClassLoader().getResource(ResourceManager.assertForwardSlash(rasterPath)) ?: return null
        val source = ImageIcon(url)                      // ImageIcon(URL) loads fully via MediaTracker
        if (source.iconWidth <= 0) return null
        // Render synchronously into BufferedImage variants (a 1x base + a 2x HiDPI variant, both
        // downscaled straight from the often-512px source). getScaledInstance would be async and
        // leave the icon unloaded (-1x-1) when painted off the EDT / offscreen.
        val image: Image = BaseMultiResolutionImage(
            renderScaled(source.image, size),
            renderScaled(source.image, size * 2)
        )
        return ImageIcon(image).also { rasterCache[key] = it }
    }

    private fun renderScaled(source: Image, size: Int): BufferedImage {
        val out = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(source, 0, 0, size, size, null)
        g.dispose()
        return out
    }

    private fun kebab(name: String): String = name
        .replace('_', '-')
        .replace(Regex("([a-z0-9])([A-Z])"), "$1-$2")
        .replace(Regex("([A-Z]+)([A-Z][a-z])"), "$1-$2")
        .lowercase()
        .replace(Regex("-+"), "-")
        .trim('-')
}
