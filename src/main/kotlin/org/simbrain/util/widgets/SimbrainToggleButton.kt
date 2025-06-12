package org.simbrain.util.widgets

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JToggleButton
import javax.swing.UIManager

class SimbrainToggleButton(
    text: String? = null,
    icon: Icon? = null,
    private val stateGetter: (() -> Boolean)? = null,
    private val stateSetter: ((Boolean) -> Unit)? = null,
    private val tooltipGenerator: ((Boolean) -> String)? = null
) : JToggleButton(text, icon) {
    
    init {
        setupStyling()
        
        // Set up state management if functions are provided
        if (stateGetter != null && stateSetter != null && tooltipGenerator != null) {
            updateFromExternalState()
            
            // Handle button clicks
            addActionListener {
                val newState = isSelected
                stateSetter(newState)
                updateFromExternalState()
            }
        }
    }
    
    /**
     * Updates the button state from external changes
     */
    fun updateFromExternalState() {
        if (stateGetter != null && tooltipGenerator != null) {
            isSelected = stateGetter()
            toolTipText = tooltipGenerator(isSelected)
        }
    }
    
    private fun setupStyling() {
        // Start with flat appearance with light gray border (like default JButton)
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color(160, 160, 160), 1), // Light gray 1px border
            BorderFactory.createEmptyBorder(1, 2, 1, 2) // Inner padding
        )
        
        // Remove default button styling
        isContentAreaFilled = false
        isFocusPainted = false
        
        // Override the UI painting
        addChangeListener {
            updateBorder()
        }
    }
    
    private fun updateBorder() {
        border = when {
            isSelected -> BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(160, 160, 160), 1), // Keep light gray border
                BorderFactory.createEmptyBorder(1, 2, 1, 2) // Inner lowered border
            )
            model.isRollover -> BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(160, 160, 160), 1), // Keep light gray border
                BorderFactory.createEmptyBorder(1, 2, 1, 2) // Inner raised border
            )
            else -> BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(160, 160, 160), 1), // Light gray 1px border
                BorderFactory.createEmptyBorder(1, 2, 1, 2) // Inner padding
            )
        }
    }
    
    override fun paintComponent(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Paint background
        val bgColor = when {
            isSelected -> Color(200, 200, 200) // Darker when active/selected
            model.isPressed -> Color(220, 220, 220) // Slightly darker when pressed
            model.isRollover -> Color(240, 240, 240) // Light gray on hover
            else -> UIManager.getColor("Button.background") ?: Color(238, 238, 238)
        }
        
        g2d.color = bgColor
        g2d.fillRect(0, 0, width, height)
        
        // Paint the button content (icon and/or text)
        super.paintComponent(g)
    }
}