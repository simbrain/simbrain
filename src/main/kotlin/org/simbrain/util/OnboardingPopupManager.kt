package org.simbrain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.simbrain.workspace.WorkspacePreferences
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * A utility class for creating hovering onboarding popups with "Do not show again" functionality.
 * These popups can be anchored to specific components and conditionally shown based on user preferences.
 */
class OnboardingPopupManager(private val rootFrame: JFrame) {
    
    private val glassPane = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g.create() as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            
            activePopups.forEach { popup ->
                popup.paint(g2)
            }
            
            g2.dispose()
        }
    }.apply {
        isOpaque = false
        layout = null
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                handleGlassPaneMouseEvent(e)
            }
            override fun mousePressed(e: MouseEvent) {
                handleGlassPaneMouseEvent(e)
            }
        })
    }
    private val activePopups = mutableListOf<OnboardingPopup>()
    
    init {
        rootFrame.glassPane = glassPane
    }
    
    /**
     * Show a popup if conditions are met and it hasn't been dismissed
     */
    suspend fun showPopup(config: PopupConfig) = withContext(Dispatchers.Swing) {
        // Check if user has dismissed this popup
        if (config.suppressionKey != null && WorkspacePreferences.isPopupSuppressed(config.suppressionKey)) {
            return@withContext
        }
        
        // Check conditional trigger
        if (!config.condition()) {
            return@withContext
        }
        
        // Find target component bounds relative to the glass pane
        val targetBounds = if (config.targetComponent != null) {
            // Force a layout pass to ensure component is positioned
            config.targetComponent.validate()
            rootFrame.validate()
            
            // Use SwingUtilities.convertRectangle for reliable coordinate conversion
            try {
                SwingUtilities.convertRectangle(
                    config.targetComponent.parent ?: config.targetComponent,
                    config.targetComponent.bounds,
                    glassPane
                )
            } catch (e: Exception) {
                // Fallback to screen coordinates if SwingUtilities fails
                try {
                    if (config.targetComponent.isShowing && config.targetComponent.isDisplayable) {
                        val componentLocationOnScreen = config.targetComponent.locationOnScreen
                        val glassPaneLocationOnScreen = glassPane.locationOnScreen
                        
                        val relativeX = componentLocationOnScreen.x - glassPaneLocationOnScreen.x
                        val relativeY = componentLocationOnScreen.y - glassPaneLocationOnScreen.y
                        
                        Rectangle(relativeX, relativeY, config.targetComponent.width, config.targetComponent.height)
                    } else {
                        // Final fallback: use component bounds directly
                        config.targetComponent.bounds
                    }
                } catch (e2: Exception) {
                    // Last resort: use component bounds
                    config.targetComponent.bounds
                }
            }
        } else {
            Rectangle(config.position.x, config.position.y, 0, 0)
        }
        
        val popup = OnboardingPopup(config, targetBounds) { dismissedPopup ->
            activePopups.remove(dismissedPopup)
            glassPane.repaint()
            if (activePopups.isEmpty()) {
                glassPane.isVisible = false
            }
        }
        
        activePopups.add(popup)
        glassPane.isVisible = true
        glassPane.repaint()
    }
    
    /**
     * Dismiss all active popups
     */
    fun dismissAll() {
        activePopups.clear()
        glassPane.isVisible = false
        glassPane.repaint()
    }
    
    /**
     * Handle mouse events on the glass pane
     */
    private fun handleGlassPaneMouseEvent(e: MouseEvent) {
        val point = e.point
        for (popup in activePopups.reversed()) { // Check in reverse order (top to bottom)
            if (popup.contains(point)) {
                val handled = popup.handleMouseEvent(e)
                if (handled) {
                    glassPane.repaint()
                    e.consume()
                    return
                }
            }
        }
        // If no popup handled the event, pass it through by making glass pane temporarily invisible
        glassPane.isVisible = false
        val component = SwingUtilities.getDeepestComponentAt(rootFrame.contentPane, e.x, e.y)
        if (component != null) {
            val convertedEvent = SwingUtilities.convertMouseEvent(glassPane, e, component)
            component.dispatchEvent(convertedEvent)
        }
        glassPane.isVisible = activePopups.isNotEmpty()
    }
}

/**
 * Configuration for an onboarding popup
 */
data class PopupConfig(
    val title: String,
    val message: String,
    val targetComponent: JComponent? = null,
    val position: Point = Point(100, 100),
    val placement: PopupPlacement = PopupPlacement.BOTTOM_RIGHT,
    val suppressionKey: String? = null,
    val showDoNotShowAgain: Boolean = true,
    val condition: () -> Boolean = { true },
    val width: Int = 300,
    val maxWidth: Int = 400,
    val style: PopupStyle = PopupStyle.DEFAULT
)

/**
 * Where to place the popup relative to the target component
 */
enum class PopupPlacement {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, TOP_CENTER, BOTTOM_CENTER, LEFT, RIGHT
}

/**
 * Visual style of the popup
 */
enum class PopupStyle {
    DEFAULT, INFO, WARNING, SUCCESS
}

/**
 * Individual popup instance
 */
private class OnboardingPopup(
    private val config: PopupConfig,
    private val targetBounds: Rectangle,
    private val onDismiss: (OnboardingPopup) -> Unit
) {
    
    private val padding = 16
    private val borderRadius = 8
    private val shadowOffset = 4
    private val arrowSize = 12
    
    private var bounds: Rectangle
    private var doNotShowAgainChecked = false
    private var closeButtonBounds: Rectangle
    private var checkboxBounds: Rectangle
    private var checkboxTextBounds: Rectangle
    
    init {
        val metrics = getFontMetrics()
        val textBounds = calculateTextBounds(metrics)
        bounds = calculatePopupBounds(textBounds)
        closeButtonBounds = Rectangle(bounds.x + bounds.width - 20 - 8, bounds.y + 8, 20, 20)
        
        // Position checkbox at bottom
        val checkboxY = bounds.y + bounds.height - padding - 16
        checkboxBounds = Rectangle(bounds.x + padding, checkboxY, 16, 16)
        checkboxTextBounds = Rectangle(bounds.x + padding + 20, checkboxY, 120, 16)
    }
    
    private fun getFontMetrics(): FontMetrics {
        val temp = JLabel()
        return temp.getFontMetrics(temp.font)
    }
    
    private fun calculateTextBounds(metrics: FontMetrics): Dimension {
        val titleHeight = metrics.height + 4
        val messageLines = wrapText(config.message, config.maxWidth - padding * 2, metrics)
        val messageHeight = messageLines.size * metrics.height
        val checkboxHeight = if (config.showDoNotShowAgain) 24 else 0
        
        return Dimension(
            minOf(config.width, config.maxWidth),
            titleHeight + messageHeight + checkboxHeight + padding * 3
        )
    }
    
    private fun calculatePopupBounds(textBounds: Dimension): Rectangle {
        val popupWidth = textBounds.width + padding * 2
        val popupHeight = textBounds.height + padding * 2
        
        return when (config.placement) {
            PopupPlacement.BOTTOM_RIGHT -> Rectangle(
                targetBounds.x + targetBounds.width + 10,
                targetBounds.y + targetBounds.height + 10,
                popupWidth, popupHeight
            )
            PopupPlacement.BOTTOM_LEFT -> Rectangle(
                targetBounds.x - popupWidth - 10,
                targetBounds.y + targetBounds.height + 10,
                popupWidth, popupHeight
            )
            PopupPlacement.TOP_RIGHT -> Rectangle(
                targetBounds.x + targetBounds.width + 10,
                targetBounds.y - popupHeight - 10,
                popupWidth, popupHeight
            )
            PopupPlacement.TOP_LEFT -> Rectangle(
                targetBounds.x - popupWidth - 10,
                targetBounds.y - popupHeight - 10,
                popupWidth, popupHeight
            )
            PopupPlacement.TOP_CENTER -> Rectangle(
                targetBounds.x + (targetBounds.width - popupWidth) / 2,
                targetBounds.y - popupHeight - arrowSize - 5,
                popupWidth, popupHeight
            )
            PopupPlacement.BOTTOM_CENTER -> Rectangle(
                targetBounds.x + (targetBounds.width - popupWidth) / 2,
                targetBounds.y + targetBounds.height + arrowSize + 5,
                popupWidth, popupHeight
            )
            PopupPlacement.LEFT -> Rectangle(
                targetBounds.x - popupWidth - arrowSize - 5,
                targetBounds.y + (targetBounds.height - popupHeight) / 2,
                popupWidth, popupHeight
            )
            PopupPlacement.RIGHT -> Rectangle(
                targetBounds.x + targetBounds.width + arrowSize + 5,
                targetBounds.y + (targetBounds.height - popupHeight) / 2,
                popupWidth, popupHeight
            )
        }
    }
    
    private fun wrapText(text: String, maxWidth: Int, metrics: FontMetrics): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (metrics.stringWidth(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    lines.add(word) // Single word too long, add anyway
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        return lines
    }
    
    fun paint(g2: Graphics2D) {
        // Draw shadow
        g2.color = Color(0, 0, 0, 50)
        g2.fillRoundRect(
            bounds.x + shadowOffset, bounds.y + shadowOffset,
            bounds.width, bounds.height,
            borderRadius, borderRadius
        )
        
        // Draw popup background
        val backgroundColor = when (config.style) {
            PopupStyle.DEFAULT -> Color(248, 249, 250)
            PopupStyle.INFO -> Color(240, 248, 255)
            PopupStyle.WARNING -> Color(255, 248, 240)
            PopupStyle.SUCCESS -> Color(240, 255, 240)
        }
        
        g2.color = backgroundColor
        g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, borderRadius, borderRadius)
        
        // Draw border
        val borderColor = when (config.style) {
            PopupStyle.DEFAULT -> Color(200, 200, 200)
            PopupStyle.INFO -> Color(100, 150, 255)
            PopupStyle.WARNING -> Color(255, 150, 100)
            PopupStyle.SUCCESS -> Color(100, 255, 150)
        }
        
        g2.color = borderColor
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, borderRadius, borderRadius)
        
        // Draw arrow if targeting a component
        if (config.targetComponent != null) {
            drawArrow(g2, backgroundColor, borderColor)
        }
        
        // Draw content
        drawContent(g2)
        
        // Draw close button
        drawCloseButton(g2)
        
        // Draw checkbox if enabled
        if (config.showDoNotShowAgain) {
            drawCheckbox(g2)
        }
    }
    
    private fun drawArrow(g2: Graphics2D, backgroundColor: Color, borderColor: Color) {
        val arrowPoints = when (config.placement) {
            // Arrow tip points toward component below (popup is above component)
            PopupPlacement.TOP_CENTER -> arrayOf(
                Point(bounds.x + bounds.width / 2, bounds.y + bounds.height + arrowSize), // tip points down
                Point(bounds.x + bounds.width / 2 - arrowSize / 2, bounds.y + bounds.height), // left base
                Point(bounds.x + bounds.width / 2 + arrowSize / 2, bounds.y + bounds.height)  // right base
            )
            // Arrow tip points toward component above (popup is below component)  
            PopupPlacement.BOTTOM_CENTER -> arrayOf(
                Point(bounds.x + bounds.width / 2, bounds.y - arrowSize), // tip points up
                Point(bounds.x + bounds.width / 2 - arrowSize / 2, bounds.y), // left base
                Point(bounds.x + bounds.width / 2 + arrowSize / 2, bounds.y)  // right base
            )
            // Arrow tip points toward component on right (popup is left of component)
            PopupPlacement.LEFT -> arrayOf(
                Point(bounds.x + bounds.width + arrowSize, bounds.y + bounds.height / 2), // tip points right
                Point(bounds.x + bounds.width, bounds.y + bounds.height / 2 - arrowSize / 2), // top base
                Point(bounds.x + bounds.width, bounds.y + bounds.height / 2 + arrowSize / 2)  // bottom base
            )
            // Arrow tip points toward component on left (popup is right of component)
            PopupPlacement.RIGHT -> arrayOf(
                Point(bounds.x - arrowSize, bounds.y + bounds.height / 2), // tip points left
                Point(bounds.x, bounds.y + bounds.height / 2 - arrowSize / 2), // top base
                Point(bounds.x, bounds.y + bounds.height / 2 + arrowSize / 2)  // bottom base
            )
            else -> null
        }
        
        arrowPoints?.let { points ->
            val xPoints = points.map { it.x }.toIntArray()
            val yPoints = points.map { it.y }.toIntArray()
            
            g2.color = backgroundColor
            g2.fillPolygon(xPoints, yPoints, 3)
            g2.color = borderColor
            g2.drawPolygon(xPoints, yPoints, 3)
        }
    }
    
    private fun drawContent(g2: Graphics2D) {
        var y = bounds.y + padding
        
        // Draw title
        g2.color = Color.BLACK
        g2.font = g2.font.deriveFont(Font.BOLD, 14f)
        g2.drawString(config.title, bounds.x + padding, y + g2.fontMetrics.ascent)
        y += g2.fontMetrics.height + 4
        
        // Draw message
        g2.font = g2.font.deriveFont(Font.PLAIN, 12f)
        val metrics = g2.fontMetrics
        val messageLines = wrapText(config.message, bounds.width - padding * 2, metrics)
        
        for (line in messageLines) {
            g2.drawString(line, bounds.x + padding, y + metrics.ascent)
            y += metrics.height
        }
    }
    
    private fun drawCloseButton(g2: Graphics2D) {
        g2.color = Color.GRAY
        g2.stroke = BasicStroke(2f)
        
        val x = closeButtonBounds.x
        val y = closeButtonBounds.y
        val size = closeButtonBounds.width
        
        // Draw X
        g2.drawLine(x + 4, y + 4, x + size - 4, y + size - 4)
        g2.drawLine(x + size - 4, y + 4, x + 4, y + size - 4)
    }
    
    private fun drawCheckbox(g2: Graphics2D) {
        // Draw checkbox
        g2.color = Color.WHITE
        g2.fillRect(checkboxBounds.x, checkboxBounds.y, checkboxBounds.width, checkboxBounds.height)
        g2.color = Color.GRAY
        g2.drawRect(checkboxBounds.x, checkboxBounds.y, checkboxBounds.width, checkboxBounds.height)
        
        if (doNotShowAgainChecked) {
            g2.color = Color.BLACK
            g2.stroke = BasicStroke(2f)
            // Draw checkmark
            g2.drawLine(checkboxBounds.x + 3, checkboxBounds.y + 8, checkboxBounds.x + 6, checkboxBounds.y + 11)
            g2.drawLine(checkboxBounds.x + 6, checkboxBounds.y + 11, checkboxBounds.x + 12, checkboxBounds.y + 4)
        }
        
        // Draw text
        g2.color = Color.BLACK
        g2.font = g2.font.deriveFont(Font.PLAIN, 11f)
        g2.drawString("Do not show again", checkboxTextBounds.x, checkboxTextBounds.y + 12)
    }
    
    fun contains(point: Point): Boolean {
        return bounds.contains(point)
    }
    
    fun handleMouseEvent(e: MouseEvent): Boolean {
        // Handle both clicked and pressed events for better responsiveness
        if (e.id == MouseEvent.MOUSE_CLICKED || e.id == MouseEvent.MOUSE_PRESSED) {
            when {
                closeButtonBounds.contains(e.point) -> {
                    if (e.id == MouseEvent.MOUSE_CLICKED) { // Only dismiss on actual click
                        dismiss()
                    }
                    return true
                }
                config.showDoNotShowAgain && 
                (checkboxBounds.contains(e.point) || checkboxTextBounds.contains(e.point)) -> {
                    if (e.id == MouseEvent.MOUSE_CLICKED) { // Only toggle on actual click
                        doNotShowAgainChecked = !doNotShowAgainChecked
                        if (doNotShowAgainChecked && config.suppressionKey != null) {
                            WorkspacePreferences.suppressPopup(config.suppressionKey)
                        }
                    }
                    return true
                }
                // Handle clicks anywhere else in the popup (but don't dismiss)
                bounds.contains(e.point) -> {
                    return true // Consume the event but don't take action
                }
            }
        }
        return false
    }
    
    private fun dismiss() {
        onDismiss(this)
    }
}
