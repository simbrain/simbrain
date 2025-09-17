package org.simbrain.workspace.gui;

import org.simbrain.util.BuildInfo;
import org.simbrain.util.ResourceManager;
import smile.math.blas.BLAS;

import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * @(#)Splasher.java  2.0  January 31, 2004
 *
 * Copyright (c) 2003-2004 Werner Randelshofer
 * Staldenmattweg 2, Immensee, CH-6405, Switzerland.
 * All rights reserved.
 *
 * This software is in the public domain.
 */

/**
 * <b>Splasher</b> displays the simbrain splash screen an initializes the
 * workspace.
 */
public class Splasher {
    /**
     * Shows the splash screen, launches the application and then disposes the
     * splash screen.
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {

        System.setProperty("sun.java2d.metal", "true");
        
        // Set macOS-specific properties for the application name in the menu bar
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", BuildInfo.INSTANCE.getApplicationTitle());
        System.setProperty("apple.awt.application.name", BuildInfo.INSTANCE.getApplicationTitle());

        // Set up loggers (other logging config for tinylog is in build.gradle)
        Logger.getLogger("com.jme").setLevel(Level.OFF);
        Logger.getLogger("com.jmex").setLevel(Level.OFF);

        // TODO: Consider adding a progress bar to show what's being loaded

        // Hack to force initialization of Smile matrix engine at startup and remove subsequent delays
        BLAS.engine.iamax(new float[]{1f, 2f, 3f});

        // Same hack as above, for deep net engine
        // if (!Utils.isMacOSX() && !Utils.isLinux()) {
        //     var dummyDeepNet = Sequential.of(List.of(new Input(new long[]{1L},""), new Dense()),false);
        // }

        SplashWindow.splash(ResourceManager.getImage("simbrain-logo.gif"));
        SplashWindow.invokeMain("org.simbrain.workspace.gui.SimbrainDesktop", args);
        SplashWindow.disposeSplash();

    }
}
