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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.simbrain.network.nodes.NeuronNode;
import org.simbrain.network.nodes.ScreenElement;
import org.simbrain.network.nodes.SynapseNode;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.NetworkListener;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;

import edu.umd.cs.piccolo.PNode;


/**
 * Buffer which holds network objects.
 */
public class Clipboard {

    /** Static list of cut or copied objects. */
    private static ArrayList clipboard = new ArrayList();

    /** List of components which listen for changes to this clipboard. */
    private static HashSet listenerList = new HashSet();

    /** Distance between pasted elemeents. */
    private static final int PASTE_INCREMENT = 15;

    /**
     * Clear the clipboard.
     */
    public static void clear() {
        clipboard = new ArrayList();
        fireClipboardChanged();
    }

    /**
     * Add objects to the clipboard.
     *
     * @param objects objects to add
     */
    public static void add(final ArrayList objects) {
        clipboard.addAll(objects);
        clipboard = copyClipboard();
        fireClipboardChanged();
    }

    /**
     * Paste objects into the netPanel.
     *
     * @param net the network to paste into
     */
    public static void paste(final NetworkPanel net) {

        if (isEmpty()) {
            return;
        }

        ArrayList clipboardCopy = copyClipboard();
        ArrayList toSelect = new ArrayList();
        translateObjects(clipboardCopy, net);

        for (int i = 0; i < clipboardCopy.size(); i++) {
            ScreenElement element = (ScreenElement) clipboardCopy.get(i);
            if (element instanceof NeuronNode) {
                NeuronNode clipboardViewNeuron = (NeuronNode) element;
                Neuron modelNeuron = clipboardViewNeuron.getNeuron();
                net.getNetwork().addNeuron(modelNeuron);

                NeuronNode newViewNeuron = net.findNeuronNode(modelNeuron);
                newViewNeuron.setOffset(clipboardViewNeuron.getOffset());
                toSelect.add(newViewNeuron);
            }
        }
        for (int i = 0; i < clipboardCopy.size(); i++) {
            ScreenElement element = (ScreenElement) clipboardCopy.get(i);
            if (element instanceof SynapseNode) {
                SynapseNode clipboardViewSynapse = (SynapseNode) element;
                Synapse modelSynapse = clipboardViewSynapse.getSynapse();
                net.getNetwork().addWeight(modelSynapse);

                SynapseNode newViewSynapse = net.findSynapseNode(modelSynapse);
                toSelect.add(newViewSynapse);
            }
        }

        // Select just pasted items
        net.setSelection(toSelect);
        net.repaint();
    }

    /**
     * @return true if there's nothing in the clipboard, false otherwise
     */
    public static boolean isEmpty() {
        return clipboard.isEmpty();
    }

    /**
     * Make a copy of the clipboard.
     *
     * @return the copy of the clipboard
     */
    private static ArrayList copyClipboard() {
        ArrayList ret = new ArrayList();

        // Used to associate copied NeuronNodes with their originals so that
        //   synapse nodes can be connected appopriately
        Hashtable nodeMappings = new Hashtable();

        for (int i = 0; i < clipboard.size(); i++) {
            ScreenElement oldNode = (ScreenElement) clipboard.get(i);

            if (oldNode instanceof NeuronNode) {
                NeuronNode newNeuronNode = new NeuronNode(oldNode
                        .getGlobalTranslation().getX(), oldNode
                        .getGlobalTranslation().getY());
                newNeuronNode.setNeuron(((NeuronNode) oldNode).getNeuron().duplicate());
                nodeMappings.put(oldNode, newNeuronNode);
                ret.add(newNeuronNode);
            }
        }

        for (int i = 0; i < clipboard.size(); i++) {
            ScreenElement element = (ScreenElement) clipboard.get(i);

            if (element instanceof SynapseNode) {

                // Find the new NeuronNode source and targets corresponding to this new SynapseNode
                SynapseNode oldSynapseNode = (SynapseNode) element;
                NeuronNode newSource = (NeuronNode) nodeMappings.get(oldSynapseNode.getSource());
                NeuronNode newTarget = (NeuronNode) nodeMappings.get(oldSynapseNode.getTarget());

                // Create the copied SynapseNode
                SynapseNode newSynapseNode = new SynapseNode();
                newSynapseNode.setSource(newSource);
                newSynapseNode.setTarget(newTarget);

                // Set the model synapse
                Synapse newSynapse = oldSynapseNode.getSynapse().duplicate();
                newSynapse.setTarget(newTarget.getNeuron());
                newSynapse.setSource(newSource.getNeuron());
                newSynapseNode.setSynapse(newSynapse);

                ret.add(newSynapseNode);
            }
        }

        return ret;
    }

    /**
     * Return the upper left corner of the clipbard objects.
     *
     * @param clip reference to clipboard
     * @return the point corresponding to the upper left corner of the objects in the clipboard
     */
    private static Point2D getUpperLeft(final ArrayList clip) {
        double centerX = 0;
        double centerY = 0;

        for (int i = 0; i < clip.size(); i++) {
            ScreenElement element = (ScreenElement) clip.get(i);

            if (element instanceof NeuronNode) {
                PNode node = (PNode) element;
                centerX = node.getGlobalTranslation().getX();
                centerY = node.getGlobalTranslation().getY();
                break;
            }
        }

        for (int i = 0; i < clip.size(); i++) {
            ScreenElement element = (ScreenElement) clip.get(i);
            PNode node = (PNode) element;

            if (element instanceof NeuronNode) {
                if (node.getGlobalTranslation().getX() < centerX) {
                    centerX = node.getGlobalTranslation().getX();
                }
                if (node.getGlobalTranslation().getY() < centerY) {
                    centerY = node.getGlobalTranslation().getY();
                }
            }
        }

        return new Point2D.Double(centerX, centerY);
    }


    /**
     * Move the designated objects over based on number of pastes that have occurred in the specified network.
     *
     * @param clip the list of things to paste
     * @param net reference to the NetworkPanel
     */
    public static void translateObjects(final ArrayList clip, final NetworkPanel net) {
        Point2D center = null;

        try {
            center = getUpperLeft(clip);
        } catch (ClassCastException bie) {
            System.out.println("Can not calculate center point, use default position for paste");
            bie.printStackTrace();
        }

        for (int i = 0; i < clip.size(); i++) {
            ScreenElement element = (ScreenElement) clip.get(i);

            if (element instanceof NeuronNode) {
                if (net.getNumberOfPastes() == 0) {
                    ((NeuronNode) element).translate(net.getLastClickedPosition().getX() - center.getX(),
                                                     net.getLastClickedPosition().getY() - center.getY());
                } else {
                    ((NeuronNode) element).translate(net.getLastClickedPosition().getX() - center.getX()
                                                      + (net.getNumberOfPastes() * PASTE_INCREMENT),
                                                     net.getLastClickedPosition().getY() - center.getY()
                                                      + (net.getNumberOfPastes() * PASTE_INCREMENT));
                }
            }
        }
    }

    /**
     * Determines whether or not a pnode can be copied.  Free-floating weights, for example, cannot be copied.
     *
     * @param node node to be checked
     * @param netPanel reference to NetworkPanel to be copied from
     * @return true if this node can be copied, false otherwise
     */
    public static boolean canBeCopied(final PNode node, final NetworkPanel netPanel) {
        if (node instanceof NeuronNode) {
            return true;
        }

        if (node instanceof SynapseNode) {
            SynapseNode w = (SynapseNode) node;

            if (w.getSource() != null) {
                if (netPanel.getSelection().contains(w.getSource())
                        && netPanel.getSelection().contains(w.getTarget())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Add the specified clipboard listener.
     *
     * @param l listener to add
     */
    public static void addClipboardListener(final ClipboardListener l) {
        listenerList.add(l);
    }

    /**
     * Fire a clipboard changed event to all registered model listeners.
     */
    public static void fireClipboardChanged() {
        for (Iterator i = listenerList.iterator(); i.hasNext(); ) {
            ClipboardListener listener = (ClipboardListener) i.next();
            listener.clipboardChanged();
        }
    }
}
