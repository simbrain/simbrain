package org.simbrain.util.genericframe;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

/**
 * Abstraction which is neutral between JFrames, JInternalFrames, and JDialogs.
 *
 * @author jyoshimi
 */
public interface GenericFrame {

    void dispose();

    void pack();

    void setTitle(String title);

    String getTitle();

    void setIcon(boolean b) throws PropertyVetoException;

    void setJMenuBar(JMenuBar menuBar);

    JMenuBar getJMenuBar();

    void setBounds(int x, int y, int width, int height);

    Rectangle getBounds();

    void setVisible(boolean isVisible);

    void setBounds(Rectangle bounds);

    void setLocationRelativeTo(Component c);

    void setLocation(int xposition, int yposition);

    void setContentPane(Container container);

    void setMaximumSize(Dimension maximumSize);

    Dimension getMaximumSize();

    Dimension getSize();

    Dimension getPreferredSize();

    void setPreferredSize(Dimension dim);

    void setResizable(boolean b);

    void setMaximizable(boolean b);

    void toFront();
}
