package org.simbrain.util.genericframe;

import org.simbrain.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * JInternalFrame which implements Generic Frame. Renders with rounded corners so desktop windows
 * match the app's rounded look instead of being the only square-cornered surface. The frame stays
 * opaque (no flicker): the rounded-away corners are filled with the desktop color so they blend
 * into the background, and the card (title bar + content) is clipped to the rounded shape. While
 * maximized the frame renders square and full-bleed.
 */
public class GenericJInternalFrame extends JInternalFrame implements GenericFrame {

    /** Corner arc (px), matching the app's component arc family. */
    private static final float ARC = 12f;

    public GenericJInternalFrame() {
        // No shadow / inset: the content fills the frame and the resize grip stays at the visible edge.
        setBorder(new EmptyBorder(0, 0, 0, 0));
        addPropertyChangeListener(evt -> {
            if (JInternalFrame.IS_MAXIMUM_PROPERTY.equals(evt.getPropertyName())) {
                if (Boolean.TRUE.equals(evt.getNewValue())) {
                    var width = getMaximumSize().width;
                    if (width != Integer.MAX_VALUE) {
                        // Frame is being maximized
                        setSize(getMaximumSize());
                        validate(); // Make sure the frame layout is updated
                    }
                }
            }
        });
    }

    /**
     * Re-root every descendant repaint to this frame so the rounded clip, corner fill and outline
     * (painted in {@link #paintComponent}/{@link #paintChildren}) are re-applied on top. Without this,
     * an opaque child repainting itself directly (the title pane on selection, the content canvas while
     * a sim runs) paints to the frame's edge and erases our outline/corners. Disabled while maximized,
     * where the frame renders square and full-bleed and needs no rounding.
     */
    @Override
    protected boolean isPaintingOrigin() {
        return !isMaximum();
    }

    private RoundRectangle2D.Float cardShape() {
        return new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), ARC, ARC);
    }

    private Color desktopColor() {
        Container parent = getParent();
        if (parent != null && parent.getBackground() != null) {
            return parent.getBackground();
        }
        Color c = UIManager.getColor("Desktop.background");
        return c != null ? c : getBackground();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (isMaximum()) {
            super.paintComponent(g);
            return;
        }
        RoundRectangle2D.Float card = cardShape();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Fill the whole (opaque) bounds with the desktop color so the rounded-away corners blend in.
        g2.setColor(desktopColor());
        g2.fillRect(0, 0, getWidth(), getHeight());
        // Card background, with the title band tinted to match the (themed) title bar so the rounded
        // top corners do not show the content background.
        g2.setColor(getBackground());
        g2.fill(card);
        Component north = (getUI() instanceof BasicInternalFrameUI ui) ? ui.getNorthPane() : null;
        Color titleBg = UIManager.getColor(isSelected()
            ? "InternalFrame.activeTitleBackground" : "InternalFrame.inactiveTitleBackground");
        if (north != null && titleBg != null) {
            g2.clip(card);
            g2.setColor(titleBg);
            g2.fillRect(0, 0, getWidth(), north.getHeight());
        }
        g2.dispose();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (isMaximum()) {
            super.paintChildren(g);
            return;
        }
        RoundRectangle2D.Float card = cardShape();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.clip(card);
        super.paintChildren(g2);
        g2.dispose();
        // Rounded outline, drawn above the children so the title bar / content edges stay inside it.
        Graphics2D g3 = (Graphics2D) g.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(Theme.getDivider());
        g3.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
        g3.dispose();
    }

    @Override
    public void setLocationRelativeTo(Component c) {
    }

    @Override
    public void toFront() {
        super.toFront();
    }
}
