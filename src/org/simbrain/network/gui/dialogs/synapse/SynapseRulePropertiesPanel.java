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
package org.simbrain.network.gui.dialogs.synapse;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JLabel;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.util.Parameter;
import org.simbrain.util.UserParameter;
import org.simbrain.util.widgets.ParameterWidget;

/**
 * A synapse rule parameter editing panel for synapse update rules that use
 * parameter fields annotated with {@link UserParameter}. Automatically adds
 * fields for all UserParameter-annotated fields. May be sub-classed to add
 * custom fields or behaviour.
 * 
 * @author O. J. Coleman
 */
public class SynapseRulePropertiesPanel extends AbstractSynapseRulePanel {

    /**
     * The available parameters, as a map from Parameter to input gui component.
     */
    protected Set<ParameterWidget> params;

    /**
     * The prototype rule.
     */
    protected SynapseUpdateRule prototypeRule;

    /**
     * Create a new SynapseRuleUserParamPanel for the given rule.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public SynapseRulePropertiesPanel(SynapseUpdateRule prototypeRule) {
        this();
        setRule(prototypeRule);
    }

    /**
     * Create a new SynapseRuleUserParamPanel with no rule set.
     * {@link #setRule(SynapseUpdateRule)} must be set before other methods are
     * called. Mostly for internal use.
     */
    public SynapseRulePropertiesPanel() {
    }

    public void setRule(SynapseUpdateRule prototypeRule) {
        if (this.prototypeRule != null) {
            throw new IllegalStateException(
                    "Multiple calls to SynapseRuleUserParamPanel.setRule(SynapseUpdateRule) are not allowed.");
        }

        this.prototypeRule = prototypeRule;

        params = new TreeSet<>();
        for (Parameter param : Parameter
                .getParameters(prototypeRule.getClass())) {
            params.add(new ParameterWidget(param));
        }

        // Add parameter widgets after collecting list of params so they're in
        // the right order.
        for (ParameterWidget pw : params) {
            JLabel label = new JLabel(pw.parameter.annotation.label());
            label.setToolTipText(pw.getToolTipText());
            this.addItemLabel(label, pw.component);
        }
    }

    @Override
    public SynapseRulePropertiesPanel deepCopy() {
        SynapseRulePropertiesPanel copy;

        try {
            copy = this.getClass().getConstructor().newInstance();
            // If a (sub-class) constructor didn't call
            // SynapseRuleUserParamPanel(SynapseUpdateRule)
            if (copy.prototypeRule == null) {
                copy.setRule(this.prototypeRule);
            }
        } catch (InstantiationException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException(
                    "The class " + this.getClass().getName()
                            + " must declare a constructor accepting no arguments (or override the deepCopy() method).",
                    e);
        }

        // Iterate over both sets of parameters. They should be in the same
        // order,
        // unless something very odd is going on with the loaded classes.
        Iterator<ParameterWidget> thisParamsItr = params.iterator();
        Iterator<ParameterWidget> copyParamsItr = copy.params.iterator();
        while (thisParamsItr.hasNext()) {
            ParameterWidget thisPW = thisParamsItr.next();
            ParameterWidget copyPW = copyParamsItr.next();
            assert (thisPW.equals(copyPW));
            copyPW.setWidgetValue(thisPW.getWidgetValue());
        }
        return copy;
    }

    @Override
    public void fillFieldValues(List<SynapseUpdateRule> ruleList) {
        SynapseUpdateRule refSynapse = ruleList.get(0);

        for (ParameterWidget pw : params) {
            // Check to see if the field values are consistent over all given
            // instances.
            boolean consistent = true;
            Object refValue = pw.parameter.getFieldValue(refSynapse);
            for (int i = 1; i < ruleList.size(); i++) {
                SynapseUpdateRule rule = ruleList.get(i);
                Object ruleValue = pw.parameter.getFieldValue(rule);
                if ((refValue == null && ruleValue != null)
                        || (refValue != null && !refValue.equals(ruleValue))) {
                    consistent = false;
                    break;
                }
            }

            if (!consistent) {
                pw.setWidgetValue(null);
            } else {
                pw.setWidgetValue(refValue);
            }
        }
    }

    @Override
    public void fillDefaultValues() {
        for (ParameterWidget pw : params) {
            pw.setWidgetValue(pw.parameter.getDefaultValue());
        }
    }

    @Override
    public void commitChanges(Synapse synapse) {
        if (!synapse.getLearningRule().getClass()
                .equals(prototypeRule.getClass())) {
            synapse.setLearningRule(prototypeRule.deepCopy());
        }
        writeValuesToRules(Collections.singletonList(synapse));
    }

    @Override
    public void commitChanges(Collection<Synapse> synapses) {
        if (isReplace()) {
            for (Synapse s : synapses) {
                // Only replace if this is a different rule (otherwise when
                // editing multiple rules with different parameter values which
                // have not been set those values will be replaced with the
                // default).
                if (!s.getLearningRule().getClass()
                        .equals(prototypeRule.getClass())) {
                    s.setLearningRule(prototypeRule.deepCopy());
                }
            }
        }
        writeValuesToRules(synapses);
    }

    @Override
    protected void writeValuesToRules(Collection<Synapse> synapses) {
        for (ParameterWidget pw : params) {
            Object value = pw.getWidgetValue();

            // Ignore unspecified values.
            // If the value isn't null and it's either not a String or not an
            // empty string.
            if (value != null && (!(value instanceof String)
                    || !((String) value).equals(""))) {
                for (Synapse s : synapses) {
                    pw.parameter.setFieldValue(s.getLearningRule(), value);
                }
            }
        }
        // Re-initialise. Allows updating cached values calculated from
        // parameters.
        for (Synapse s : synapses) {
            s.getLearningRule().init(s);
        }
    }

    @Override
    public SynapseUpdateRule getPrototypeRule() {
        return prototypeRule;
    }
}
