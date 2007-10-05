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
package org.simbrain.gauge.graphics;

import java.awt.event.KeyEvent;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;


/**
 * <b>KeyEventHandler</b> handles key events to the GaugePanel.
 */
public class KeyEventHandler extends PBasicInputEventHandler {
    /** Gauge panel. */
    private GaugePanel gp;

    /**
     * @param gaugePanel Gauge to handle key events.
     */
    public KeyEventHandler(final GaugePanel gaugePanel) {
        gp = gaugePanel;
    }

    /**
     * @param e Responds to key inputs.
     */
    public void keyPressed(final PInputEvent e) {
        int keycode = e.getKeyCode();

        switch (keycode) {
            case KeyEvent.VK_H:
                gp.getGauge().getUpstairs().printDataset();
                break;

            case KeyEvent.VK_L:
                gp.getGauge().getDownstairs().printDataset();

                break;

            case KeyEvent.VK_D:
                //System.out.println(gp.getGauge().getGaugedVars());
                break;

            default:
                break;
        }
    }

    /**
     * @param gp Gauge panel to respond to.
     */
    public void setGp(final GaugePanel gp) {
        this.gp = gp;
    }

    /**
     * @return Gauge panel to respond to.
     */
    public GaugePanel getGp() {
        return this.gp;
    }
}
