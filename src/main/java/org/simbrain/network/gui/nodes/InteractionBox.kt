package org.simbrain.network.gui.nodes

import org.piccolo2d.PCamera
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
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

/**
 * Interaction Box: graphical element for interacting with a group. Subclasses support custom menus, dialogs, etc.
 */
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
     * This is the largest amount an interaction box's scale can be zoomed when the scale gets small. Easiest to
     * understand by changing the value and "zooming  out" of a network containing a neuron group.
     */
    private val largestZoomRescaleFactor = 4.0

    /**
     * Reference to property change listener so it can be cleaned up later.
     */
    val zoomListener: PropertyChangeListener = PropertyChangeListener { evt: PropertyChangeEvent? ->
        val viewScale = networkPanel.canvas.camera.viewScale
        if (viewScale < 1) {
            // Rescale based on linear equation so that as view scale
            // goes from 1 to 0 rescaleAmount goes from 1 to "largestZoomRescaleFactor"
            val rescaleAmount = (1 - largestZoomRescaleFactor) * viewScale + largestZoomRescaleFactor
            setScale(rescaleAmount)
        } else {
            setScale(1.0)
        }
    }

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

        networkPanel.canvas.camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM, zoomListener)
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
        // Calculate total width needed (text + padding + menu + padding)
        // Use fullBounds to account for the 0.8x scale transform applied to the text
        val textBounds = textLabel.fullBounds
        val menuBounds = kebabMenu.bounds  // Now using regular bounds since we set them explicitly
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