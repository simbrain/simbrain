/**
 * The label box on a network element: a tab showing the element's name, with a kebab that opens its menu. Subclasses
 * supply the menu, property dialog, and tooltip. The box is as wide as its label, up to [InteractionBox.maxWidth],
 * and scales with the rest of the canvas when the view zooms.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.blend
import java.awt.Color
import java.awt.geom.Rectangle2D

abstract class InteractionBox(networkPanel: NetworkPanel) : ScreenElement(networkPanel) {

    private val textLabel: PText
    private val kebabMenu: PNode
    private val dots = mutableListOf<PPath>()

    /** Tab fill color; subclasses override to tint the tab (e.g. supervised models). */
    protected open val tabFillColor: Color get() = NetworkTheme.current.tabFill

    /**
     * Padding values for layout
     */
    private val paddingX = 4.0
    private val paddingY = 2.0
    private val kebabPaddingX = 6.0
    private val kebabPaddingY = 2.0

    /**
     * Rectangle that displays the box.
     */
    private val rect: Rectangle2D = Rectangle2D.Float(0f, 0f, DEFAULT_WIDTH, DEFAULT_HEIGHT)

    /**
     * Create a new tab node.
     */
    init {
        this.append(rect, false)
        textLabel = PText().apply { font = Theme.body }
        addChild(textLabel)

        // Create kebab menu button
        kebabMenu = createKebabMenu()
        addChild(kebabMenu)

        applyThemeColors()
    }

    /** Re-apply the themed tab colors (fill, border, label, kebab dots). */
    protected fun applyThemeColors() {
        setPaint(tabFillColor)
        setStrokePaint(NetworkTheme.current.subnetOutline)
        textLabel.textPaint = NetworkTheme.current.tabText
        dots.forEach { it.setPaint(blend(NetworkTheme.current.tabText, tabFillColor, 0.55)) }
    }

    override fun refreshTheme() {
        applyThemeColors()
    }

    /**
     * Creates a three-dot menu icon with vertical dots (kebab style)
     */
    private fun createKebabMenu(): PNode {
        val menuNode = PNode()
        val dotSize = 1.5
        val dotSpacing = 1.0
        val dotsHeight = (dotSize * 3) + (dotSpacing * 2)
        
        // Set explicit bounds for the menu container with padding for hover area
        menuNode.setBounds(0.0, 0.0, dotSize + (kebabPaddingX * 2), dotsHeight + (kebabPaddingY * 2))
        
        // Calculate starting position for dots
        val dotsStartX = kebabPaddingX
        val dotsStartY = kebabPaddingY

        // Create three circular dots, arranged vertically (colors applied by applyThemeColors)
        for (i in 0..2) {
            val dot = PPath.createEllipse(
                dotsStartX,
                dotsStartY + i * (dotSize + dotSpacing),
                dotSize,
                dotSize
            )
            dot.setStroke(null) // No border, just filled circles
            dots.add(dot)
            menuNode.addChild(dot)
        }

        // Add input event handlers for click and hover (hover colors read fresh from the theme)
        menuNode.addInputEventListener(object : PBasicInputEventHandler() {
            override fun mouseClicked(event: PInputEvent) {
                onKebabMenuClicked(event)
            }

            override fun mouseEntered(event: PInputEvent) {
                dots.forEach { it.setPaint(NetworkTheme.current.tabText) }
            }

            override fun mouseExited(event: PInputEvent) {
                dots.forEach { it.setPaint(blend(NetworkTheme.current.tabText, tabFillColor, 0.55)) }
            }
        })
        
        return menuNode
    }
    
    /**
     * Override this method in subclasses to handle kebab menu clicks
     */
    protected open fun onKebabMenuClicked(event: PInputEvent) {
        contextMenu?.show(networkPanel, event.canvasPosition.x.toInt(), event.canvasPosition.y.toInt())
    }

    /** The label as given to [setText], which is cut short where it does not fit in [maxWidth]. */
    private var fullText = " "

    /**
     * Widest the box is, so that it does not crowd its neighbors. A label that does not fit has its middle replaced
     * with an ellipsis, keeping the start and the end, where default names like "WeightMatrix_3" differ.
     */
    var maxWidth = kotlin.Double.POSITIVE_INFINITY
        set(value) {
            if (value == field) return
            field = value
            updateLayout()
        }

    /**
     * Set text for interaction box.
     *
     * @param text the textLabel to set
     */
    fun setText(text: String?) {
        var text = text
        if (text == null) {
            return
        }

        if (text.isEmpty()) {
            text = " " // Use a blank string rather than an empty string so that the box does not disappear
        }
        fullText = text
        textLabel.setText(text)

        // Make smaller than interaction box if scale is 1
        // (Otherwise it keeps getting smaller when editing)
        if (textLabel.getScale() == 1.0) {
            textLabel.scaleAboutPoint(
                .8, bounds.center2D.x,
                bounds.center2D.y
            )
        }

        updateLayout()
    }

    /**
     * Updates the layout of text and kebab menu within the bounds
     */
    private fun updateLayout() {
        val menuBounds = kebabMenu.bounds  // Now using regular bounds since we set them explicitly
        fitText(maxWidth - paddingX * 2 - menuBounds.width)

        // Calculate total width needed (text + padding + menu + padding)
        // Use fullBounds to account for the 0.8x scale transform applied to the text
        val textBounds = textLabel.fullBounds
        val totalWidth = textBounds.width + paddingX + menuBounds.width + paddingX
        val totalHeight = maxOf(textBounds.height, menuBounds.height) + paddingY * 2
        
        // Set the interaction box bounds
        val newBounds = Rectangle2D.Double(bounds.x, bounds.y, totalWidth, totalHeight)
        setBounds(newBounds)
        
        // Position text on the left with padding
        textLabel.setOffset(paddingX, (totalHeight - textBounds.height) / 2)
        
        // Position kebab menu on the right with padding
        kebabMenu.setOffset(
            totalWidth - menuBounds.width,
            (totalHeight - menuBounds.height) / 2
        )
    }

    /** Show as much of [fullText] as fits in [room], with an ellipsis in place of the middle if any is left out. */
    private fun fitText(room: kotlin.Double) {
        textLabel.text = fullText
        var shown = fullText.length
        while (textLabel.fullBounds.width > room && shown > 2) {
            shown--
            val head = shown / 2
            textLabel.text = fullText.take(head).trimEnd() + "\u2026" + fullText.takeLast(shown - head).trimStart()
        }
    }

    override val isDraggable: Boolean = false

    public override fun acceptsSourceHandle(): Boolean {
        return true
    }

    companion object {
        /**
         * Width of interaction box.
         */
        private const val DEFAULT_WIDTH = 20f

        /**
         * Height of interaction box.
         */
        private const val DEFAULT_HEIGHT = 10f
    }
}