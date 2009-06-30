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
package org.simbrain.plot.projection;

import javax.swing.SwingUtilities;

/**
 * Update iterable projection algorithms
 */
public class ProjectionUpdater implements Runnable {

    /** Reference to projection component. */
    private ProjectionComponent component;

    /**
     * @param component
     */
    public ProjectionUpdater(ProjectionComponent component) {
        super();
        this.component = component;
    }

    /**
     * {@inheritDoc}
     */
    public void run() {
        while (!component.isRunning()) {
            try {
                component.setUpdateCompleted(false);
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        component.getGauge().iterate(1);
                        component.resetChartDataset();
                        component.fireUpdateEvent();
                    }
                });
                while (!component.isUpdateCompleted()) {
                    Thread.sleep(1); // TODO: make this settable?
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
