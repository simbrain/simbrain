package org.simbrain.util.genericframe;

import org.simbrain.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * JInternalFrame which implements Generic Frame. Renders with rounded corners so desktop windows
 * match the app's rounded look instead of being the only square-cornered surface. The frame is
 * non-opaque and the rounded-away corners are antialiased-cleared, so the desktop — or an
 * overlapping frame behind it — shows through them with a smooth edge (true transparency, not a
 * fill matched to the background).
 *
 * <p>Soft-clipping the corners needs an offscreen buffer, but only corner-touching repaints take
 * that path: a repaint whose dirty region does not reach a corner paints straight to the screen,
 * so a running sim repainting the canvas interior stays at full speed. The corner buffer is sized
 * to device pixels and blitted back 1:1, so corners stay crisp on HiDPI displays. While maximized
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
     * Drop-in for {@link JInternalFrame#JInternalFrame(String, boolean, boolean, boolean, boolean)} so call
     * sites that build a plain internal frame can switch to the rounded one with no other change.
     */
    public GenericJInternalFrame(String title, boolean resizable, boolean closable,
                                 boolean maximizable, boolean iconifiable) {
        this();
        setTitle(title);
        setResizable(resizable);
        setClosable(closable);
        setMaximizable(maximizable);
        setIconifiable(iconifiable);
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
        // Fill the card; the rounded corners are antialiased-cleared afterwards in paint().
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        Component north = (getUI() instanceof BasicInternalFrameUI ui) ? ui.getNorthPane() : null;
        Color titleBg = UIManager.getColor(isSelected()
            ? "InternalFrame.activeTitleBackground" : "InternalFrame.inactiveTitleBackground");
        if (north != null && titleBg != null) {
            g.setColor(titleBg);
            g.fillRect(0, 0, getWidth(), north.getHeight());
        }
    }

    @Override
    public void paint(Graphics g) {
        if (isMaximum()) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g;
        Rectangle clip = g2.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(0, 0, getWidth(), getHeight());
        }
        // Fast path: a repaint that does not reach a corner needs no rounding, so paint straight to
        // the screen with no offscreen buffer. Only corner-touching repaints pay for soft-clipping.
        if (!clipTouchesCorner(clip)) {
            super.paint(g);
            paintOutline(g);
            return;
        }
        AffineTransform at = g2.getTransform();
        double scale = at.getScaleX() > 0 ? at.getScaleX() : 1;
        int bufW = Math.max(1, (int) Math.ceil(clip.width * scale));
        int bufH = Math.max(1, (int) Math.ceil(clip.height * scale));
        BufferedImage buf = new BufferedImage(bufW, bufH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D bg = buf.createGraphics();
        bg.scale(scale, scale);
        bg.translate(-clip.x, -clip.y);
        bg.setClip(clip);
        boolean wasDoubleBuffered = isDoubleBuffered();
        setDoubleBuffered(false);
        try {
            super.paint(bg);
        } finally {
            setDoubleBuffered(wasDoubleBuffered);
        }
        // Antialiased corner clear: knock the rounded-away corners out of the buffer so the edge is
        // smooth and the desktop / overlapping frame shows through.
        bg.setComposite(AlphaComposite.Clear);
        bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        bg.fill(cornerCutouts());
        bg.dispose();
        // Blit the device-resolution buffer back 1:1 (no upscaling, so corners stay crisp on HiDPI).
        Graphics2D gd = (Graphics2D) g.create();
        gd.setTransform(new AffineTransform());
        int devX = (int) Math.round(at.getTranslateX() + clip.x * scale);
        int devY = (int) Math.round(at.getTranslateY() + clip.y * scale);
        gd.drawImage(buf, devX, devY, null);
        gd.dispose();
        paintOutline(g);
    }

    private void paintOutline(Graphics g) {
        Graphics2D g3 = (Graphics2D) g.create();
        g3.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g3.setColor(Theme.getDivider());
        g3.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1f, getHeight() - 1f, ARC, ARC));
        g3.dispose();
    }

    private Area cornerCutouts() {
        // Clear a hair inside the card edge so the antialiased content edge tucks under the outline
        // stroke (drawn in paintOutline) instead of leaking a sliver of color past it at the corners,
        // where the content arc and the half-pixel-inset outline arc otherwise diverge.
        float inset = 1f;
        Area cutouts = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
        cutouts.subtract(new Area(new RoundRectangle2D.Float(
            inset, inset, getWidth() - 2 * inset, getHeight() - 2 * inset, ARC, ARC)));
        return cutouts;
    }

    private boolean clipTouchesCorner(Rectangle clip) {
        int w = getWidth();
        int h = getHeight();
        int a = (int) Math.ceil(ARC);
        return clip.intersects(0, 0, a, a)
            || clip.intersects(w - a, 0, a, a)
            || clip.intersects(0, h - a, a, a)
            || clip.intersects(w - a, h - a, a, a);
    }

    @Override
    public void setLocationRelativeTo(Component c) {
    }

    @Override
    public void toFront() {
        super.toFront();
    }
}
