package org.simbrain.util.widgets;

import org.simbrain.util.ResourceManager;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.util.propertyeditor.Parameter;
import org.simbrain.util.propertyeditor.ParameterWidget;
import org.simbrain.util.stats.ProbabilityDistribution;
import org.simbrain.util.stats.distributions.NormalDistribution;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Used in the {@link ParameterWidget} for numeric
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
    private JButton randomizeButton = new JButton(ResourceManager.getImageIcon("menu_icons/Rand.png"));

    /**
     * Class associated with the underlying parameter to facilitate type conversions. Currently only used for real
     * valued numbers
     */
    private Class<?> paramClass;

    /**
     * Construct a numeric widget.
     *
     * @param editableObjects the objects being edited
     * @param parameter       the parameter field
     * @param spinnerModel    the spinner model
     * @param setNull         a function to set the field to null (inconsistent) when the randomize button is clicked
     */
    public NumericWidget(
            List<? extends EditableObject> editableObjects,
            Parameter parameter,
            SpinnerNumberModelWithNull spinnerModel,
            Runnable setNull
    ) {
        super();
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

        spinner = new JNumberSpinnerWithNull(spinnerModel);
        gridBagConstraints.weightx = 10;
        add(spinner, gridBagConstraints);

        randomizeButton.setToolTipText("Randomize this parameter. Note that upon pressing OK the value" +
            " will be updated immediately.");
        String probDist =parameter.getAnnotation().probDist();

        paramClass = parameter.getType().getClass();

        // Handle randomizer button
        if (!probDist.isEmpty()) {
            randomizeButton.addActionListener((evt) -> {

                ProbabilityDistribution dist = new NormalDistribution();

                AnnotatedPropertyEditor randEditor =
                        new AnnotatedPropertyEditor(new ProbabilityDistribution.Randomizer(dist));
                StandardDialog dialog = new StandardDialog();
                dialog.setContentPane(randEditor);
                dialog.pack();

                dialog.addClosingTask(() -> {
                    editableObjects.forEach(o -> {
                        if (parameter.isNumericInteger()) {
                            parameter.setFieldValue(o, dist.sampleInt());
                        } else {
                            parameter.setFieldValue(o, dist.sampleDouble());
                        }
                    });
                    if(editableObjects.size() == 1) {
                        setValue(parameter.getFieldValue(editableObjects.get(0)));
                    } else {
                        // Provides some indication that fields have been mutated
                        if (setNull != null) {
                            setNull.run();
                        }
                    }
                });

                dialog.setLocationRelativeTo(null);
                dialog.setVisible(true);
                dialog.isAlwaysOnTop();

            });
            gridBagConstraints.weightx = 2;
            add(randomizeButton, gridBagConstraints);
        }
    }

    public Object getValue() {
        if (paramClass == Float.class || paramClass == float.class) {
            return Float.valueOf("" + spinner.getValue());
        }
        if (paramClass == Double.class || paramClass == double.class) {
            return Double.valueOf("" + spinner.getValue());
        }
        if (paramClass == Integer.class || paramClass == int.class) {
            return Integer.valueOf("" + spinner.getValue());
        }
        return spinner.getValue();
    }

    public boolean isNull() {
        return spinner.getValue() == null;
    }

    public void setValue(Object value) {
        spinner.setValue(value);
    }

    @Override
    public void setEnabled(boolean enabled) {
        spinner.setEnabled(enabled);
        spinner.getEditor().setEnabled(enabled);
    }
}
