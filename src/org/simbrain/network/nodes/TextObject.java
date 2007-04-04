package org.simbrain.network.nodes;

import java.awt.Color;
import java.awt.Point;
import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.nodes.PText;
import edu.umd.cs.piccolo.util.PBounds;

/**
 * An editable text object.
 */
public class TextObject extends ScreenElement {

    /** Reference to parent NetworkPanel. */
    private NetworkPanel networkPanel;

    /** The text object. */
    private PText ptext;

    /**
     * Construct text object at specified location.
     *
     * @param netPanel reference to networkPanel
     * @param p point at which to place text object
     */
    public TextObject(final NetworkPanel netPanel, final Point2D p) {
        super(netPanel);
        networkPanel = netPanel;
        this.addInputEventListener(new TextEventHandler());
        ptext = new PText();
        offset(p.getX(), p.getY());
        ptext.setText("Text");
        //this.setPaint(Color.red);
        this.addChild(ptext);
    }

    public boolean setBounds(PBounds bounds) {
        return ptext.setBounds(bounds);
    }

    public PBounds getBounds() {
        return ptext.getBounds();
    }

    /**
     * Reset time on double clicks.
     */
    private class TextEventHandler
        extends PBasicInputEventHandler {

        /** @see PBasicInputEventHandler */
        public void mousePressed(final PInputEvent event) {
            if (event.getClickCount() == 2) {
                networkPanel.getRootNetwork().setTime(0);
                ptext.setText(JOptionPane.showInputDialog("Text:"));
            }
        }

    }

    @Override
    public boolean isSelectable() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean showSelectionHandle() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isDraggable() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    protected boolean hasToolTipText() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected String getToolTipText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean hasContextMenu() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected JPopupMenu getContextMenu() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected boolean hasPropertyDialog() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected JDialog getPropertyDialog() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void resetColors() {
        // TODO Auto-generated method stub
        
    }
}
