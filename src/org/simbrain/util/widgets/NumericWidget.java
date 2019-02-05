package org.simbrain.util.widgets;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.math.ProbDistributions.UniformDistribution;
import org.simbrain.util.math.ProbabilityDistribution;
import org.simbrain.util.propertyeditor2.AnnotatedPropertyEditor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class NumericWidget extends JPanel {

    JNumberSpinnerWithNull field;

    JButton randomizeButton = new JButton("Rand");

    Consumer<ProbabilityDistribution> applyRandom;

    public NumericWidget(SpinnerNumberModelWithNull spinnerModel, Consumer<ProbabilityDistribution> applyRandom) {
        super();
        setLayout(new GridBagLayout());
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        field = new JNumberSpinnerWithNull(spinnerModel);
        gridBagConstraints.weightx = 10;
        add(field, gridBagConstraints);
        if (applyRandom != null) {
            this.applyRandom = applyRandom;
            randomizeButton.addActionListener(evt -> {
                applyRandom.accept(UniformDistribution.builder().lowerBound(0).upperBound(10).build());
            });
            gridBagConstraints.weightx = 2;
            add(randomizeButton, gridBagConstraints);
        }
    }

    Object getValue() {
        return field.getValue();
    }

    void setValue(Object value) {
        field.setValue(value);
    }

}
