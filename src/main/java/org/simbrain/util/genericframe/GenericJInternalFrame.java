package org.simbrain.util.genericframe;

import org.simbrain.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * JInternalFrame which implements Generic Frame. Renders with rounded corners so desktop windows
 * match the app's rounded look instead of being the only square-cornered surface. The frame is
 * non-opaque and the rounded-away corners are left unpainted, so the desktop — or an overlapping
 * frame behind it — shows through them (true transparency, not a fill matched to the background).
 * The card (title bar + content) is hard-clipped to the rounded shape; corners are aliased rather
 * than antialiased, which avoids an offscreen compositing buffer on every repaint. While maximized
 * the frame renders square and full-bleed.
 */
public class GenericJInternalFrame extends JInternalFrame implements GenericFrame {

    /** Corner arc (px), matching the app's component arc family. */
    private static final float ARC = 12f;

    public GenericJInternalFrame() {
        // No shadow / inset: the content fills the frame and the resize grip stays at the visible edge.
        setBorder(new EmptyBorder(0, 0, 0, 0));
        // Non-opaque so the rounded-away corners are see-through, not a fill matched to the background.
        setOpaque(false);
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
     * Re-root every descendant repaint to this frame so the rounded clip and outline
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

    @Override
    protected void paintComponent(Graphics g) {
        if (isMaximum()) {
            super.paintComponent(g);
            return;
        }
        RoundRectangle2D.Float card = cardShape();
        Graphics2D g2 = (Graphics2D) g.create();
        // Hard-clip to the rounded card; corners outside it are left unpainted, so the desktop or an
        // overlapping frame behind shows through (the frame is non-opaque).
        g2.clip(card);
        // Card background, with the title band tinted to match the (themed) title bar so the rounded
        // top corners do not show the content background.
        g2.setColor(getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());
        Component north = (getUI() instanceof BasicInternalFrameUI ui) ? ui.getNorthPane() : null;
        Color titleBg = UIManager.getColor(isSelected()
            ? "InternalFrame.activeTitleBackground" : "InternalFrame.inactiveTitleBackground");
        if (north != null && titleBg != null) {
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
