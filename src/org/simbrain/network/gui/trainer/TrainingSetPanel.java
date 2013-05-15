/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.simbrain.network.core.Neuron;
import org.simbrain.network.gui.trainer.DataPanel.DataMatrix;
import org.simbrain.util.genericframe.GenericFrame;

/**
 * Display input data and target data.
 *
 * @author jyoshimi
 */
public class TrainingSetPanel extends JPanel {

    /** Parent frame. */
    private GenericFrame parentFrame;

    DataPanel inputPanel;
    DataPanel targetPanel;
    /**
     * Construct the training set panel.
     *
     * TODO: This will change soon, since it will display a training set object.
     */
    public TrainingSetPanel(List<Neuron> inputNeurons,
            final DataMatrix inputData, final List<Neuron> outputNeurons,
            final DataMatrix targetData) {

        inputPanel = new DataPanel(
                inputNeurons, inputData, "Input data");
        inputPanel.getScroller().setMaxVisibleColumns(4);
        targetPanel = new DataPanel(
                outputNeurons, targetData, "Input data");
        targetPanel.getScroller().setMaxVisibleColumns(4);

        final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input data"));
        targetPanel.setBorder(BorderFactory.createTitledBorder("Target data"));
        // split.setDividerLocation(.5);
        split.setLeftComponent(inputPanel);
        split.setRightComponent(targetPanel);
        split.setResizeWeight(.5);
        add(split);
        split.setBorder(null);

//        addComponentListener(new ComponentAdapter() {
//            public void componentResized(ComponentEvent evt) {
//                resizePanels();
//            }
//        });
    }

    /**
     * Hacked code for resizing sub-panels as this panel is resized.
     */
    private void resizePanels() {
        inputPanel.setPreferredSize(new Dimension(getWidth() / 2 - 20,
                inputPanel.getHeight()));
        inputPanel.revalidate();
        targetPanel.setPreferredSize(new Dimension(getWidth() / 2 - 20,
                targetPanel.getHeight()));
        targetPanel.revalidate();

    }

    /**
     * Set the frame.
     *
     * @param frame frame to set
     */
    public void setFrame(GenericFrame frame) {
        parentFrame = frame;
        //parentFrame.setResizable(false);
        parentFrame.setMaximizable(false);
        inputPanel.setFrame(frame);
        targetPanel.setFrame(frame);
    }

}
