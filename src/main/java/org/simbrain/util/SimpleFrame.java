package org.simbrain.util;

import javax.swing.*;

/**
 * Displays a simple JFrame, centers it, packs it, and makes it visible.
 *
 * @author jyoshimi
 */
public class SimpleFrame {

    /**
     * Show the provided panel in a jframe.
     *
     * @param component the component to show.
     */
    public static void displayPanel(JComponent component) {
        displayPanel(component, null);
    }

    /**
     * Show the panel in a jframe with the provided title.
     *
     * @param component the component to show
     * @param string    the title
     */
    public static void displayPanel(JComponent component, String string) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(component);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setTitle(string);
    }

}
