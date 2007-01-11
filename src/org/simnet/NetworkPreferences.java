/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * <b>NetworkPreferences</b> handles storage and retrieval of User Preferences for the
 * neural network.
 */
public final class NetworkPreferences {

    /** The main user preference object. */
    private static final Preferences PREFERENCES = Preferences.userRoot().node("/org/simbrain/simnet");

    /** Default activation threshold preference setting. */
    private static final double DEFAULT_ACTIVATION_THRESHOLD = 0.5d;

    /** Default neuron increment preference setting. */
    private static final double DEFAULT_NEURON_INCREMENT = 0.1d;

    /** Default neuron lower bound preference setting. */
    private static final double DEFAULT_NEURON_LOWER_BOUND = -1.0d;

    /** Default neuron upper bound preference setting. */
    private static final double DEFAULT_NEURON_UPPER_BOUND = 1.0d;

    /** Default output threshold preference setting. */
    private static final double DEFAULT_OUTPUT_THRESHOLD = 0.5d;

    /** Default activation function preference setting. */
    private static final String DEFAULT_ACTIVATION_FUNCTION = "Linear";

    /** Default output function preference setting. */
    private static final String DEFAULT_OUTPUT_FUNCTION = "Threshold";

    /** Default weight increment preference setting. */
    private static final double DEFAULT_WEIGHT_INCREMENT = 0.1d;

    /** Default weight increment preference setting. */
    private static final double DEFAULT_WEIGHT_LOWER_BOUND = -1.0d;

    /** Default weight increment preference setting. */
    private static final double DEFAULT_WEIGHT_UPPER_BOUND = 1.0d;

    /** Default weight learning rule preference setting. */
    private static final String DEFAULT_LEARNING_RULE = "Hebbian";

    /** Default momentum preference setting. */
    private static final double DEFAULT_MOMENTUM = 0.2d;

    /** Default connection setting. */
    private static final String DEFAULT_CONNECTION_TYPE = "All To All";


    /**
     * Private default constructor.
     */
    private NetworkPreferences() {
        // empty
    }


    /**
     * Save all user preferences.
     */
    public static void saveAll() {
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    //------------ Neuron Preferences --------------------//

    /**
     * Return the activation function preference setting.
     * Defaults to <code>&quot;Linear&quot;</code>.
     *
     * @return the activation function preference setting
     */
    public static String getActivationFunction() {
        return PREFERENCES.get("activationFunction", DEFAULT_ACTIVATION_FUNCTION);
    }

    /**
     * Set the activation function preference setting to <code>s</code>.
     *
     * @param s activation function preference setting
     */
    public static void setActivationFunction(final String s) {
        PREFERENCES.put("activationFunction", s);
    }

    /**
     * Return the output function preference setting.
     * Defaults to <code>&quot;Threshold&quot;</code>.
     *
     * @return the output function preference setting
     */
    public static String getOutputFunction() {
        return PREFERENCES.get("outputFunction", DEFAULT_OUTPUT_FUNCTION);
    }

    /**
     * Set the output function preference setting to <code>s</code>.
     *
     * @param s output function preference setting
     */
    public static void setOutputFunction(final String s) {
        PREFERENCES.put("outputFunction", s);
    }

    /**
     * Return the activation preference setting.
     * Defaults to <code>0.0d</code>.
     *
     * @return the activation preference setting
     */
    public static double getActivation() {
        return PREFERENCES.getDouble("Activation", 0.0d);
    }

    /**
     * Set the activation preference setting to <code>d</code>.
     *
     * @param d activation preference setting
     */
    public static void setActivation(final double d) {
        PREFERENCES.putDouble("Activation", d);
    }

    /**
     * Return the activation threshold preference setting.
     * Defaults to <code>0.5d</code>.
     *
     * @return the activation threshold preference setting
     */
    public static double getActivationThreshold() {
        return PREFERENCES.getDouble("activationThreshold", DEFAULT_ACTIVATION_THRESHOLD);
    }

    /**
     * Set the activation preference setting to <code>d</code>.
     *
     * @param d activation preference setting
     */
    public static void setActivationThreshold(final double d) {
        PREFERENCES.putDouble("activationThreshold", d);
    }

    /**
     * Return the bias preference setting.
     * Defaults to <code>0.0d</code>.
     *
     * @return the bias preference setting
     */
    public static double getBias() {
        return PREFERENCES.getDouble("Bias", 0.0d);
    }

    /**
     * Set the bias preference setting to <code>d</code>.
     *
     * @param d bias preference setting
     */
    public static void setBias(final double d) {
        PREFERENCES.putDouble("Bias", d);
    }

    /**
     * Return the decay preference setting.
     * Defaults to <code>0.0d</code>.
     *
     * @return the decay preference setting
     */
    public static double getDecay() {
        return PREFERENCES.getDouble("Decay", 0.0d);
    }

    /**
     * Set the decay preference setting to <code>d</code>.
     *
     * @param d decay preference setting
     */
    public static void setDecay(final double d) {
        PREFERENCES.putDouble("Decay", d);
    }

    /**
     * Return the neuron increment preference setting.
     * Defaults to <code>0.1d</code>.
     *
     * @return the neuron increment preference setting
     */
    public static double getNrnIncrement() {
        return PREFERENCES.getDouble("nrnIncrement", DEFAULT_NEURON_INCREMENT);
    }

    /**
     * Set the neuron increment preference setting to <code>d</code>.
     *
     * @param d neuron increment preference setting
     */
    public static void setNrnIncrement(final double d) {
        PREFERENCES.putDouble("nrnIncrement", d);
    }

    /**
     * Return the neuron lower bound preference setting.
     * Defaults to <code>-1.0d</code>.
     *
     * @return the neuron lower bound preference setting
     */
    public static double getNrnLowerBound() {
        return PREFERENCES.getDouble("nrnLowerBound", DEFAULT_NEURON_LOWER_BOUND);
    }

    /**
     * Set the neuron lower bound preference setting to <code>d</code>.
     *
     * @param d neuron lower bound preference setting
     */
    public static void setNrnLowerBound(final double d) {
        PREFERENCES.putDouble("nrnLowerBound", d);
    }

    /**
     * Return the output signal preference setting.
     * Defaults to <code>1.0d</code>.
     *
     * @return the output signal preference setting
     */
    public static double getOutputSignal() {
        return PREFERENCES.getDouble("outputSignal", 1.0d);
    }

    /**
     * Set the output signal preference setting to <code>d</code>.
     *
     * @param d output signal preference setting
     */
    public static void setOutputSignal(final double d) {
        PREFERENCES.putDouble("outputSignal", d);
    }

    /**
     * Return the output threshold preference setting.
     * Defaults to <code>0.5d</code>.
     *
     * @return the output threshold preference setting
     */
    public static double getOutputThreshold() {
        return PREFERENCES.getDouble("outputThreshold", DEFAULT_OUTPUT_THRESHOLD);
    }

    /**
     * Set the output threshold preference setting to <code>d</code>.
     *
     * @param d output threshold preference setting
     */
    public static void setOutputThreshold(final double d) {
        PREFERENCES.putDouble("outputThreshold", d);
    }

    /**
     * Return the neuron upper bound preference setting.
     * Defaults to <code>1.0d</code>.
     *
     * @return the neuron upper bound preference setting
     */
    public static double getNrnUpperBound() {
        return PREFERENCES.getDouble("nrnUpperBound", DEFAULT_NEURON_UPPER_BOUND);
    }

    /**
     * Set the neuron upper bound preference setting to <code>d</code>.
     *
     * @param d neuron upper bound preference setting
     */
    public static void setNrnUpperBound(final double d) {
        PREFERENCES.putDouble("nrnUpperBound", d);
    }

    //------------ Weight Preferences --------------------//

    /**
     * Return the learning rule preference setting.
     * Defaults to <code>&quot;Hebbian&quot;</code>.
     *
     * @return the learning rule preference setting
     */
    public static String getLearningRule() {
        return PREFERENCES.get("learningRule", DEFAULT_LEARNING_RULE);
    }

    /**
     * Set the learning rule preference setting to <code>s</code>.
     *
     * @param s learning rule preference setting
     */
    public static void setLearningRule(final String s) {
        PREFERENCES.put("learningRule", s);
    }

    /**
     * Return the strength preference setting.
     * Defaults to <code>&quot;1.0d&quot;</code>.
     *
     * @return the strength preference setting
     */
    public static double getStrength() {
        return PREFERENCES.getDouble("strength", 1.0d);
    }

    /**
     * Set the strength preference setting to <code>d</code>.
     *
     * @param d strength preference setting
     */
    public static void setStrength(final double d) {
        PREFERENCES.putDouble("strength", d);
    }

    /**
     * Return the momentum preference setting.
     * Defaults to <code>&quot;0.2d&quot;</code>.
     *
     * @return the momentum preference setting
     */
    public static double getMomentum() {
        return PREFERENCES.getDouble("momentum", DEFAULT_MOMENTUM);
    }

    /**
     * Set the momentum preference setting to <code>d</code>.
     *
     * @param d momentum preference setting
     */
    public static void setMomentum(final double d) {
        PREFERENCES.putDouble("momentum", d);
    }

    /**
     * Return the weight increment preference setting.
     * Defaults to <code>&quot;0.1d&quot;</code>.
     *
     * @return the weight increment preference setting
     */
    public static double getWtIncrement() {
        return PREFERENCES.getDouble("wtIncrement", DEFAULT_WEIGHT_INCREMENT);
    }

    /**
     * Set the weight increment preference setting to <code>d</code>.
     *
     * @param d weight increment  preference setting
     */
    public static void setWtIncrement(final double d) {
        PREFERENCES.putDouble("wtIncrement", d);
    }

    /**
     * Return the weight lower bound preference setting.
     * Defaults to <code>&quot;-1.0d&quot;</code>.
     *
     * @return the weight lower bound preference setting
     */
    public static double getWtLowerBound() {
        return PREFERENCES.getDouble("wtLowerBound", DEFAULT_WEIGHT_LOWER_BOUND);
    }

    /**
     * Set the weight lower bound preference setting to <code>d</code>.
     *
     * @param d weight lower bound preference setting
     */
    public static void setWtLowerBound(final double d) {
        PREFERENCES.putDouble("wtLowerBound", d);
    }

    /**
     * Return the weight increment preference setting.
     * Defaults to <code>&quot;1.0d&quot;</code>.
     *
     * @return the weight increment preference setting
     */
    public static double getWtUpperBound() {
        return PREFERENCES.getDouble("wtUpperBound", DEFAULT_WEIGHT_UPPER_BOUND);
    }

    /**
     * Set the weight upper bound preference setting to <code>d</code>.
     *
     * @param d weight upper bound preference setting
     */
    public static void setWtUpperBound(final double d) {
        PREFERENCES.putDouble("wtUpperBound", d);
    }

    /**
     * Return the neuron connection type setting.
     * Defaults to <code>$quot;All to All&quot;</code>.
     *
     * @return the neuron connection type setting
     */
    public static String getConnectionType() {
        return PREFERENCES.get("connectionType", DEFAULT_CONNECTION_TYPE);
    }

    /**
     * Set the neuron connection type to <code>type</code>.
     *
     * @param type neuron connection type
     */
    public static void setConnectionType(final String type) {
        PREFERENCES.put("connectionType", type);
    }
}
