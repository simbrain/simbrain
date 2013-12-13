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
package org.simbrain.plot.projection;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.projection.ProjectCoordinate;
import org.simbrain.util.projection.Projector;

/**
 * A dialog box for setting general projector preferences.
 *
 * @author Jeff Yoshimi
 *
 */

public class ProjectionPreferencesDialog extends StandardDialog {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Text field to edit tolerance. */
    private JTextField tolerance = new JTextField("");

    /** Checkbox for auto-find mode. */
    private JCheckBox autoFind = new JCheckBox();

    /** Reference to projector being represented. */
    private final Projector projector;

    /**
     * Construct the preference dialog.
     *
     * @param projector model projector being represented
     */
    public ProjectionPreferencesDialog(Projector projector) {
        this.projector = projector;
        String toleranceToolTip = "Only add a new datapoint if it is at least this "
                + "far from an existing datapoint in the high-dim space";
        JLabel toleranceLabel = new JLabel("New datapoint tolerance");
        tolerance.setToolTipText(toleranceToolTip);
        toleranceLabel.setToolTipText(toleranceToolTip);
        mainPanel.addItemLabel(toleranceLabel, tolerance);
        if (projector.getProjectionMethod() instanceof ProjectCoordinate) {
            mainPanel.addItem("Coordinate projection auto-find mode", autoFind);
        }
        fillFieldValues();
        setContentPane(mainPanel);
    }

    /**
     * Populate fields with current data.
     */
    private void fillFieldValues() {
        tolerance.setText("" + projector.getTolerance());
        if (projector.getProjectionMethod() instanceof ProjectCoordinate) {
            autoFind.setSelected(((ProjectCoordinate) projector
                    .getProjectionMethod()).isAutoFind());
        }
    }

    @Override
    protected void closeDialogOk() {
        projector.setTolerance(Double.parseDouble(tolerance.getText()));
        if (projector.getProjectionMethod() instanceof ProjectCoordinate) {
            ((ProjectCoordinate) projector.getProjectionMethod())
                    .setAutoFind(autoFind.isSelected());
        }
        super.closeDialogOk();
    }

}
