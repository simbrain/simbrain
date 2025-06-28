package org.simbrain.util.widgets;

import org.simbrain.util.SimbrainConstants;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * A JSpinner supporting null (numeric) values, represented as "...".
 *
 * @author O. J. Coleman
 * @see SpinnerNumberModelWithNull
 */
public class JNumberSpinnerWithNull extends JSpinner {

    /** Minimum width of jtext field associated with this spinner. */
    private static final int MIN_TEXTFIELD_WIDTH = 8;

    public JNumberSpinnerWithNull(SpinnerNumberModelWithNull model) {
        super(model);
    }

    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerNumberModelWithNull) {
            return new NumberEditorWithNull(this);
        }
        return super.createEditor(model);
    }

    public static class NumberEditorWithNull extends JSpinner.NumberEditor {
        public NumberEditorWithNull(JNumberSpinnerWithNull spinner) {
            this(spinner, (DecimalFormat) NumberFormat.getNumberInstance(spinner.getLocale()));
        }

        public NumberEditorWithNull(JNumberSpinnerWithNull spinner, String decimalFormatPattern) {
            this(spinner, new DecimalFormat(decimalFormatPattern));
        }

        public NumberEditorWithNull(JSpinner spinner, DecimalFormat format) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel model)) {
                throw new IllegalArgumentException("model not a SpinnerNumberModel");
            }

            format.setMaximumFractionDigits(Integer.MAX_VALUE);
            NumberFormatter formatterEditor = new NumberEditorFormatter(model, format);
            NumberFormatter formatterDisplay = new NumberEditorFormatterWithNull(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatterEditor, formatterDisplay);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
            ftf.setHorizontalAlignment(JTextField.RIGHT);

            ftf.setColumns(MIN_TEXTFIELD_WIDTH);

//            /* TODO - initializing the column width of the text field
//             * is imprecise and doing it here is tricky because
//             * the developer may configure the formatter later.
//             */
//            try {
//                String maxString = formatterEditor.valueToString(model.getMinimum());
//                String minString = formatterEditor.valueToString(model.getMaximum());
//                ftf.setColumns(Math.max(maxString.length(), minString.length()));
//
//            } catch (ParseException e) {
//                // TBD should throw a chained error here
//            }

        }
    }

    public static class NumberEditorFormatter extends NumberFormatter {
        protected final SpinnerNumberModel model;

        public NumberEditorFormatter(SpinnerNumberModel model, NumberFormat format) {
            super(format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        public void setMinimum(Comparable min) {
            model.setMinimum(min);
        }

        public Comparable getMinimum() {
            return model.getMinimum();
        }

        public void setMaximum(Comparable max) {
            model.setMaximum(max);
        }

        public Comparable getMaximum() {
            return model.getMaximum();
        }
    }


    public static class NumberEditorFormatterWithNull extends NumberEditorFormatter {
        NumberEditorFormatterWithNull(SpinnerNumberModel model, NumberFormat format) {
            super(model, format);
        }

        @Override
        public Object stringToValue(String text) throws ParseException {
            if (text == null || text.equals("") || text.equals("...")) {
                return null;
            }
            return super.stringToValue(text);
        }

        @Override
        public String valueToString(Object value) throws ParseException {
            if (value == null) {
                return SimbrainConstants.NULL_STRING;
            }
            return super.valueToString(value);
        }
    }

    /**
     * Forward to the text field.
     */
    public void setEnabled(boolean enabled) {
        ((NumberEditorWithNull) getEditor()).getTextField().setEnabled(enabled);
    }
}
