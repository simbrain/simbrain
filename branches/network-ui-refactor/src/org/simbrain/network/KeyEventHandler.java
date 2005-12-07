/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import org.simbrain.network.pnodes.PNodeText;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;


/**
 * <b>KeyEventHandler</b> handles key events in the network panel
 */
public class KeyEventHandler extends PBasicInputEventHandler {
    private NetworkPanel netPanel;
    private MouseEventHandler netSelect;
    private PNodeText editNode;
    private boolean shiftKey = false;

    // System-specific modifier key: control on most systems, apple on apple, etc.
    private int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    public KeyEventHandler(final NetworkPanel np) {
        netPanel = np;
        netSelect = np.getHandle();
        editNode = null;
    }

    public void setEditingText(final PNodeText textNode) {
        editNode = textNode;
    }

    /**
     * Note that some key handling is taken care if via control-characters in Swing menus
     */
    public void keyPressed(final PInputEvent e) {
        int keycode = e.getKeyCode();

        //System.err.println("Keycode: " + keycode);
        if (editNode != null) {
            String nodeText = editNode.getText();

            // If the user is editing a block of text...
            switch (keycode) {
                case KeyEvent.VK_BACK_SPACE:

                    if (nodeText.length() > 0) {
                        nodeText = nodeText.substring(0, nodeText.length() - 1);
                    }

                    break;

                case KeyEvent.VK_ENTER:
                    System.err.println("Finished editing!");
                    editNode = null;

                    break;

                case KeyEvent.VK_SHIFT:
                    shiftKey = true;

                default:

                    if ((keycode >= 65) || (keycode <= 90)) {
                        String key = KeyEvent.getKeyText(keycode);

                        if (!shiftKey) {
                            key = key.toLowerCase();
                        }

                        nodeText += key;
                    }

                    break;
            }

            if (editNode != null) {
                editNode.setText(nodeText);
            }
        } else {
            switch (keycode) {
                case KeyEvent.VK_BACK_SPACE:
                case KeyEvent.VK_DELETE:
                    netPanel.deleteSelection();

                    break;

                case KeyEvent.VK_C:

                    // Only do this if the control key is NOT presssed
                    if ((modifier & e.getModifiers()) != modifier) {
                        netPanel.clearSelection();
                    }

                    break;

                case KeyEvent.VK_X:

                    // Only do this if the control key is NOT presssed
                    if ((modifier & e.getModifiers()) != modifier) {
                        netSelect.cutToClipboard();
                    }

                    break;

                case KeyEvent.VK_V:

                    // Only do this if the control key is NOT presssed
                    if ((modifier & e.getModifiers()) != modifier) {
                        netPanel.setMode(NetworkPanel.SELECTION);
                    }

                    break;

                case KeyEvent.VK_B:
                    netPanel.setMode(NetworkPanel.BUILD);

                    break;

                case KeyEvent.VK_H:
                    netPanel.setMode(NetworkPanel.PAN);

                    break;

                case KeyEvent.VK_Y:
                    netPanel.centerCamera();

                    break;

                case KeyEvent.VK_Z:

                    if (netPanel.getMode() == NetworkPanel.ZOOMIN) {
                        netPanel.setMode(NetworkPanel.ZOOMOUT);
                    } else {
                        netPanel.setMode(NetworkPanel.ZOOMIN);
                    }

                    break;

                case KeyEvent.VK_D:
                    netPanel.debug();

                    break;

                case KeyEvent.VK_P:
                    netPanel.addNeuron();

                    break;

                case KeyEvent.VK_R:
                    netPanel.randomizeSelection();

                    break;

                case KeyEvent.VK_U:
                    netPanel.unselectAll();

                    break;

                case KeyEvent.VK_S:

                    // Only do this if the control key is NOT presssed
                    if ((modifier & e.getModifiers()) != modifier) {
                        netPanel.updateNetwork();
                    }

                    break;

                case KeyEvent.VK_SPACE:

                    if (netPanel.getMode() != NetworkPanel.TEMP_SELECTION) {
                        netPanel.updateNetworkAndWorld();
                    }

                    break;

                case KeyEvent.VK_A:
                    netPanel.selectAll();

                    break;

                case KeyEvent.VK_N:

                    // Only do this if the control key is NOT presssed
                    if ((modifier & e.getModifiers()) != modifier) {
                        netPanel.selectNeurons();
                    }

                    break;

                case KeyEvent.VK_W:
                    netPanel.selectWeights();

                    break;

                case KeyEvent.VK_T:
                    netPanel.addText("test");

                    break;

                case KeyEvent.VK_LEFT:

                    if (e.isShiftDown()) {
                        netPanel.nudge(-1, 0);
                    } else {
                        netPanel.decrementSelectedObjects();
                    }

                    break;

                case KeyEvent.VK_RIGHT:

                    if (e.isShiftDown()) {
                        netPanel.nudge(1, 0);
                    } else {
                        netPanel.incrementSelectedObjects();
                    }

                    break;

                case KeyEvent.VK_UP:

                    if (e.isShiftDown()) {
                        netPanel.nudge(0, -1);
                    } else {
                        netPanel.incrementSelectedObjects();
                    }

                    break;

                case KeyEvent.VK_DOWN:

                    if (e.isShiftDown()) {
                        netPanel.nudge(0, 1);
                    } else {
                        netPanel.decrementSelectedObjects();
                    }
            }
        }

        if (netPanel.getMode() == NetworkPanel.BUILD) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                netPanel.setMode(NetworkPanel.TEMP_SELECTION);
            }
        }

        this.netPanel.getParentFrame().setChangedSinceLastSave(true);
    }

    public void keyReleased(final PInputEvent pie) {
        if (netPanel.getMode() == NetworkPanel.TEMP_SELECTION) {
            if (pie.getKeyCode() == KeyEvent.VK_SPACE) {
                netPanel.setMode(NetworkPanel.BUILD);
            }
        }
    }
}
