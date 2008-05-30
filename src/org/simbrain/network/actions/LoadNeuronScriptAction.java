/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005-2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.network.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;

import org.simbrain.util.SFileChooser;
import org.simnet.interfaces.Neuron;

/**
 * :oad custom update script. Obviously not done yet.
 */
public final class LoadNeuronScriptAction
    extends AbstractAction {

    /** Reference to neuron. */
    private Neuron theNeuron;

    /**
     * Load a custom neuron script.
     */
    public LoadNeuronScriptAction(final Neuron neuron) {
        super("Load...");
        theNeuron = neuron;
    }


    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
//        SFileChooser chooser = new SFileChooser(".", "bsh");
//        File theFile = chooser.showOpenDialog();
//        if (theFile != null) {
            //System.out.println("setting i");
            //theNeuron.setCustomUpdateScript(theFile);
//        }
    }
}