package org.simbrain.network.nodes;

/**
 * Text handler which overrides default text handler in Piccolox.
 */
import java.awt.Insets;
import java.awt.geom.Point2D;

import org.simbrain.network.NetworkPanel;

import edu.umd.cs.piccolo.PCamera;
import edu.umd.cs.piccolo.PCanvas;
import edu.umd.cs.piccolo.PNode;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolox.event.PStyledTextEventHandler;
import edu.umd.cs.piccolox.nodes.PStyledText;

public class TextHandler extends PStyledTextEventHandler {

    /** Reference to parent network. */
    private NetworkPanel net;

    /**
     *  Constructor.
     * @param canvas reference to network panel.
     */
    public TextHandler(PCanvas canvas) {
        super(canvas);
        net = (NetworkPanel) canvas;
    }

    /** Builds a TextObject on mouse clicks. */
    public void mousePressed(PInputEvent inputEvent) {
        PNode pickedNode = inputEvent.getPickedNode();
        stopEditing();
        if (pickedNode instanceof PStyledText) {
            startEditing(inputEvent, (PStyledText) pickedNode);
        }

        else if (pickedNode instanceof PCamera) {

            PStyledText newText = createText();
            TextObject textObj = new TextObject(net, newText);
            Insets pInsets = newText.getInsets();
            canvas.getLayer().addChild(textObj);
            textObj.translate(inputEvent.getPosition().getX() - pInsets.left,
                    inputEvent.getPosition().getY() - pInsets.top);
            startEditing(inputEvent, newText);

        }
    }

    /** Removes empty text objects. */
    public void stopEditing() {
        if (editedText != null) {
            editedText.getDocument().removeDocumentListener(docListener);
            editedText.setEditing(false);
            if (editedText.getDocument().getLength() == 0) {
                if (editedText.getParent() != null) {
                    editedText.getParent().removeFromParent();                    
                }
                editedText.removeFromParent();
            } else {
                editedText.syncWithDocument();
            }
            editor.setVisible(false);
            canvas.repaint();
            editedText = null;
        }

    }

}
