/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.trainer;

import java.io.InputStream;
import java.io.OutputStream;

import org.simbrain.network.NetworkComponent;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.workspace.WorkspaceComponent;

/**
 * Workspace component wrapper for a trainer object. Mainly facilitates
 * persistence, since there are currently no trainer attributes.
 */
public class TrainerComponent extends WorkspaceComponent {

    /** Data model. */
    private final Trainer trainer;

    /**
     * Create new Trainer Component from a specified model.
     * Used in deserializing.
     *
     * @param name component name
     */
    public TrainerComponent(final String name) {
        super(name);
        this.trainer = new BackpropTrainer();
        init();
    }

    /**
     * Create new Trainer Component from a specified model.
     * Used in deserializing.
     *
     * @param name component name
     * @param trainer specified trainer
     */
    public TrainerComponent(final String name, Trainer trainer) {
        super(name);
        this.trainer = trainer;
        init();
    }

    /**
     * Initialize the trainer.
     */
    private void init() {
    }


    @Override
    public Object getObjectFromKey(String objectKey) {
        // No coupling support currently, so no implementation
        return null;
    }

    @Override
    public String getKeyFromObject(Object object) {
        // No coupling support currently, so no implementation
        return null;
    }

    @Override
    public void save(OutputStream output, String format) {
        Trainer.getXStream().toXML(trainer, output);
    }

    /**
     * Opens a saved component.
     *
     * @param input stream
     * @param name name of file
     * @param format format
     * @return component to be opened
     */
    public static TrainerComponent open(InputStream input, final String name,
            final String format) {
        Trainer trainer = (Trainer) Trainer.getXStream().fromXML(input);
        return new TrainerComponent(name, trainer);
    }

    @Override
    protected void closing() {
        // TODO Auto-generated method stub
    }

    /**
     * @return the trainer
     */
    public Trainer getTrainer() {
        return trainer;
    }

}
