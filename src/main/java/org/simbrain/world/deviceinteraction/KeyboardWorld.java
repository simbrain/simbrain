package org.simbrain.world.deviceinteraction;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Amanda Pandey
 */
public class KeyboardWorld implements AttributeContainer {

    protected final List<Character> tokenDictionary = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z');

    private final List<Character> keys = new ArrayList<>();
    private final List<KeyboardListener> listeners = new ArrayList<>();


    private int position = 0;

    public List<Character> getKeys() {
        return Collections.unmodifiableList(keys);
    }

    /**
     * Threshold for displaying text.
     */
    private double displayThreshold = .5;


    public void keyPress(Character key) {
        keys.add(key);
        position = keys.size();
        fireKeyPressEvent(key);
    }

    public void keyPress(double threshold, Character key) {
        if (threshold > displayThreshold) {
            keyPress(key);
        }
    }

    public int getPosition() {
        return position;
    }

    public void addKeyboardListener(KeyboardListener listener) {
        listeners.add(listener);
    }

    private void fireKeyPressEvent(Character key) {
        for (KeyboardListener listener : listeners) {
            listener.keyPressed(key);
        }
    }

    public Character[] getTokenDictionary() {
        return tokenDictionary.stream().toArray(Character[]::new);
    }

    @Producible
    public String getText() {
        return keys.stream().map(Object::toString).collect(Collectors.joining(" "));
    }

    public void setPosition(int caretPosition, boolean fireEvent) {
        if (caretPosition <= tokenDictionary.size()) {
            position = caretPosition;
            if (fireEvent) {
                for (KeyboardListener listener : listeners) {
                    listener.positionChanged(position);
                }
            }
        }
    }

    @Consumable
    public void setPosition(int caretPosition) {
        setPosition(caretPosition, true);
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object
     */
    static XStream getXStream() {
        return Utils.getSimbrainXStream();
    }


    public interface KeyboardListener {

        /**
         * Notifies if a key is pressed
         */
        void keyPressed(Character key);

        /**
         * Position changed event
         *
         * @param position
         */
        void positionChanged(int position);

    }

}
