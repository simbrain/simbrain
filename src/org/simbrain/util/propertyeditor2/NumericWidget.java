package org.simbrain.util.propertyeditor2;

import org.simbrain.util.Parameter;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.widgets.JNumberSpinnerWithNull;
import org.simbrain.util.widgets.SpinnerNumberModelWithNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Used in the {@link org.simbrain.util.widgets.ParameterWidget} for numeric
 * fields. Has an an "up / down" spinner and the option of a randomization
 * button.
 */
public class NumericWidget extends JPanel {

    /**
     * The spinner component.
     */
    private JNumberSpinnerWithNull spinner;

    /**
     * The randomization button.
     */
    private JButton randomizeButton = new JButton(org.simbrain.resource.ResourceManager.getImageIcon("Rand.png"));

    /**
     * Construct a numeric widget.
     *
     * @param editableObjects the objects being edited
     * @param parameter       the parameter field
     * @param spinnerModel    the spinner model
     * @param pd              an optional randomizer for edited object (null if
     *                        not used)
     * @param setNull         a function to set the field to null (inconsistent) when the randomize button is clicked
     */
    public NumericWidget(
            List<? extends EditableObject> editableObjects,
            Parameter parameter,
            SpinnerNumberModelWithNull spinnerModel,
            ProbabilityDistribution pd,
            Runnable setNull
    ) {
        super();
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        spinner = new JNumberSpinnerWithNull(spinnerModel);
        gridBagConstraints.weightx = 10;
        add(spinner, gridBagConstraints);
        if (pd != null) {
            randomizeButton.addActionListener((evt) -> {
                editableObjects.forEach(o -> {
                    if (parameter.isNumericInteger()) {
                        parameter.setFieldValue(o, pd.nextRandInt());
                    } else {
                        parameter.setFieldValue(o, pd.nextRand());
                    }
                    // TODO: Check consistency for null
                });
                if (setNull != null) {
                    setNull.run();
                }
            });
            gridBagConstraints.weightx = 2;
            add(randomizeButton, gridBagConstraints);
        }
    }

    public Object getValue() {
        return spinner.getValue();
    }

    public void setValue(Object value) {
        spinner.setValue(value);
    }


}
