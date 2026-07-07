package org.simbrain.util.uisnapshot

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import org.simbrain.util.DetailTrianglePanel
import org.simbrain.util.installSimbrainFlatLafDefaults
import org.simbrain.util.installSimbrainSvgIconColors
import java.awt.Component
import java.awt.Container
import java.awt.Desktop
import java.awt.Window
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

/**
 * A single screenshot target. Implement this with a no-arg constructor and pass the FQCN
 * to the `uiSnapshot` Gradle task via `-PsnapshotDef=...`.
 *
 * Return a [Window] (JDialog/JFrame) — the harness packs it and snapshots its whole root pane,
 * so dialog button bars and menu bars are included — or any [JComponent] — the harness sizes it
 * to its preferred size and snapshots it directly.
 *
 * `build()` runs OFF the EDT so it's free to call `runBlocking { ... }` for suspend setup
 * (e.g. adding network models) without deadlocking on swing-dispatched events.
 */
interface UiSnapshotDef {
    val name: String
    fun build(): Component
}

/**
 * Install the FlatLaf theme to render under, before any Swing component is built.
 *
 * Only light is used today, but the harness is theme-parameterized so dark-mode
 * snapshots work the moment the app gains a dark theme — the foundation is in
 * place even though dark mode isn't a current product need. All themes ship in
 * the FlatLaf core artifact, so no extra dependency is required.
 */
fun setupTheme(name: String) {
    installSimbrainFlatLafDefaults()
    when (name.lowercase()) {
        "dark" -> FlatDarkLaf.setup()
        else -> FlatLightLaf.setup()
    }
    installSimbrainSvgIconColors()
}

/**
 * Recursively expand every [DetailTrianglePanel] (the "Settings ▼" disclosure
 * used by APE's ObjectWidget) so collapsed parameters render in the snapshot.
 */
fun Component.expandDetailPanels() {
    if (this is DetailTrianglePanel) setOpen(true)
    if (this is Container) components.forEach { it.expandDetailPanels() }
}

/**
 * Recursively find the first [JTabbedPane] containing a tab with the given title
 * and select it, so that tab's contents render in the snapshot.
 */
fun Component.selectTab(title: String): Boolean {
    if (this is JTabbedPane) {
        for (i in 0 until tabCount) {
            if (getTitleAt(i) == title) {
                selectedIndex = i
                return true
            }
        }
    }
    if (this is Container) {
        return components.any { it.selectTab(title) }
    }
    return false
}

fun main(args: Array<String>) {
    val fqcn = args.firstOrNull()
        ?: error("Pass FQCN of a UiSnapshotDef class as the first argument")
    val themeName = args.getOrNull(1) ?: "light"
    val scale = args.getOrNull(2)?.toDoubleOrNull()?.coerceIn(1.0, 4.0) ?: 1.0
    val openAfterRun = args.getOrNull(3)?.toBooleanStrictOrNull() ?: false

    setupTheme(themeName)

    val def = Class.forName(fqcn).getDeclaredConstructor().newInstance() as UiSnapshotDef
    val built = def.build()

    lateinit var target: Component
    var resizeToPreferred = false
    SwingUtilities.invokeAndWait {
        target = when (built) {
            is Window -> {
                built.pack()
                // Snapshot the whole root pane, not getContentPane(): StandardDialog
                // overrides getContentPane() to return only the user pane, which would
                // omit its OK/Cancel button bar (and any menu bar) from the image.
                (built as? RootPaneContainer)?.rootPane ?: built
            }
            is JComponent -> {
                if (built.width == 0 || built.height == 0) {
                    JDialog().apply {
                        contentPane = built
                        pack()
                    }
                    resizeToPreferred = true
                }
                built
            }
            else -> {
                built.size = built.preferredSize
                built
            }
        }
    }

    // Re-assert the requested theme: build() runs app code (e.g. SimbrainDesktop's
    // object initializer) that reinstalls its own global LookAndFeel, which would
    // otherwise override the theme set above. Reapply ours and refresh the realized
    // component tree so the snapshot deterministically reflects the requested theme.
    SwingUtilities.invokeAndWait {
        setupTheme(themeName)
        SwingUtilities.updateComponentTreeUI(target)
    }

    // Drain pending EDT tasks (component listeners may have queued layout/zoom work).
    SwingUtilities.invokeAndWait { }

    lateinit var outFile: File
    SwingUtilities.invokeAndWait {
        if (resizeToPreferred) {
            // The realizing dialog's pack() capped the component at screen size, silently
            // truncating oversized canvases; snapshots want the full preferred extent.
            target.size = target.preferredSize
            target.validate()
        }
        val w = target.width.coerceAtLeast(1)
        val h = target.height.coerceAtLeast(1)
        val outW = (w * scale).toInt().coerceAtLeast(1)
        val outH = (h * scale).toInt().coerceAtLeast(1)
        val img = BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        if (scale != 1.0) {
            g.scale(scale, scale)
        }
        target.paint(g)
        g.dispose()

        val outDir = File("build/ui-snapshots").apply { mkdirs() }
        val suffix = if (scale == 1.0) "" else "@${scale.toInt()}x"
        outFile = File(outDir, "${def.name}${suffix}.png")
        ImageIO.write(img, "PNG", outFile)
        println("Wrote ${outFile.absolutePath} (${outW}x${outH}, scale=$scale)")
    }

    if (openAfterRun) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Desktop.getDesktop().open(outFile)
        } else {
            System.err.println("Could not open ${outFile.absolutePath}: desktop integration is unavailable")
        }
    }

    System.exit(0)
}
