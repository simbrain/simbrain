package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.piccolo2d.nodes.PPath;
import org.piccolo2d.nodes.PText;
import org.simbrain.util.Utils;
import org.simbrain.world.odorworld.effectors.Speech;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

/**
 * Visual representation of speech effectors.
 */
public class SpeechNode extends EntityAttributeNode {

    /**
     * Reference to the effector this node is representing
     */
    private Speech effector;

    /**
     * Reference to the parent entity
     */
    private OdorWorldEntity entity;

    /**
     * The shape of this node
     */
    private PNode shape = new PNode();

    /**
     * The shape of the main part of the speech bubble
     */
    private PPath speechBubble;

    /**
     * The triangle pointing to the speaker
     */
    private PPath speechBubbleTriangle;

    /**
     * A white patch to cover the top part of the {@link #speechBubbleTriangle}.
     * Not the best way to draw a speech bubble but good enough for now.
     */
    private PPath speechBubblePatch;

    /**
     * The text graphical object
     */
    private PText speechText;

    /**
     * The data of the text
     */
    private String speechTextString;

    /**
     * The coordinate of the bottom left of the {@link #speechBubble}
     */
    private Point2D.Float speechBubbleBottomLeftLocation = new Point2D.Float(0, -15);

    /**
     * The top and bottom inner padding of the {@link #speechBubble}
     */
    private int paddingTopBottom = 5;

    /**
     * The left and right inner padding of the {@link #speechBubble}
     */
    private int paddingLeftRight = 10;

    /**
     * Construct a SpeechNode for the given effector.
     *
     * @param effector the effector to be represented by this node
     */
    public SpeechNode(Speech effector) {
        this.effector = effector;
        this.entity = effector.getParent();
        GeneralPath trianglePath = new GeneralPath();
        trianglePath.moveTo(18, 0);
        trianglePath.lineTo(3, speechBubbleBottomLeftLocation.getY() - 4);
        trianglePath.lineTo(15, speechBubbleBottomLeftLocation.getY() - 4);
        trianglePath.closePath();
        this.speechBubbleTriangle = new PPath.Float(trianglePath);
        this.speechBubblePatch = PPath.createRectangle(
                2,
                speechBubbleBottomLeftLocation.getY() - 5,
                15,
                3
        );
        this.speechBubblePatch.setStroke(new BasicStroke());
        this.speechBubblePatch.setStrokePaint(Color.white);
        this.speechText = new PText();
        updateText();
        shape.addChild(speechBubbleTriangle);
        shape.addChild(speechBubblePatch);
        shape.addChild(speechText);
        speechBubble.setStroke(new BasicStroke(1));
        addChild(shape);
        updateLocation();
        speechBubble.setPickable(false);
        speechBubbleTriangle.setPickable(false);
        speechBubblePatch.setPickable(false);
        speechText.setPickable(false);
        shape.setVisible(false);

        effector.addPropertyChangeListener(evt -> {
            if ("activationChanged".equals(evt.getPropertyName())) {
                shape.setVisible((Boolean) evt.getNewValue());
            } else if ("phraseChanged".equals(evt.getPropertyName())) {
                updateText();
            }
        });

        entity.addPropertyChangeListener(evt -> {
            if ("propertiesChanged".equals(evt.getPropertyName())) {
                updateText();
                updateLocation();
            }
        });
    }

    @Override
    public void update() {

    }

    /**
     * Update the location of this node to the center top of the entity.
     */
    public void updateLocation() {
        setOffset(entity.getEntityType().getImageWidth() / 2 - 18, 0);
    }


    /**
     * Update the text and the size speech bubble.
     */
    private void updateText() {
        if (!effector.getPhrase().equals(speechTextString)) {
            this.speechTextString = effector.getPhrase();
            speechText.setText(Utils.getWarpAroundString(speechTextString, effector.getCharactersPerRow()));
            shape.removeChild(speechBubble);
            speechBubble =
                    PPath.createRoundRectangle(
                            speechBubbleBottomLeftLocation.getX(),
                            speechBubbleBottomLeftLocation.getY() - speechText.getHeight() - (paddingTopBottom * 2),
                            speechText.getWidth() + (paddingLeftRight * 2),
                            speechText.getHeight() + (paddingTopBottom * 2),
                            8,
                            8
                    );
            this.speechText.setOffset(
                    speechBubbleBottomLeftLocation.getX() + paddingLeftRight,
                    speechBubbleBottomLeftLocation.getY() - speechText.getHeight() - paddingTopBottom);
            shape.addChild(speechBubble);
            speechBubbleTriangle.raiseToTop();
            speechBubblePatch.raiseToTop();
            speechText.raiseToTop();
        }
    }
}
