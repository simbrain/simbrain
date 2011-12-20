package org.simbrain.network.desktop;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.JButton;

import org.simbrain.network.gui.NetworkGuiSettings;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.NetworkDialog;

/**
 * Overrides the network dialog box to add features that don't work on applets, but that
 * work in the desktop, in particular anything reliant on user preferences.
 * 
 * @author jyoshimi
 *
 */
public class DesktopNetworkDialog extends NetworkDialog {

    /** Restore defaults button. */
    private JButton defaultButton = new JButton("Restore defaults");

    
    public DesktopNetworkDialog(NetworkPanel np) {
        super(np);

        defaultButton.addActionListener(this);
        addButton(defaultButton);

    }
    
    @Override
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == defaultButton) {
            System.out.println("Here");
            NetworkGuiPreferences.restoreDefaults();
            this.returnToCurrentPrefs();
        }
        super.actionPerformed(e);
    }
        
    /**
     * Restores the changed fields to their previous values.
     * Called when user cancels out of the dialog.
     */
    public void returnToCurrentPrefs() {
        NetworkGuiSettings.setBackgroundColor(new Color(NetworkGuiPreferences.
                getBackgroundColor()));
        NetworkGuiSettings.setLineColor(new Color(NetworkGuiPreferences.getLineColor()));
        NetworkGuiSettings.setHotColor(NetworkGuiPreferences.getHotColor());
        NetworkGuiSettings.setCoolColor(NetworkGuiPreferences.getCoolColor());
        NetworkGuiSettings.setExcitatoryColor(new Color(NetworkGuiPreferences.
                getExcitatoryColor()));
        NetworkGuiSettings.setInhibitoryColor(new Color(NetworkGuiPreferences.
                getInhibitoryColor()));
//        SelectionMarquee.setMarqueeColor(new Color(NetworkPreferences.getLassoColor()));
//        SelectionHandle.setSelectionColor(new Color(NetworkPreferences.getSelectionColor()));
        NetworkGuiSettings.setSpikingColor(new Color(NetworkGuiPreferences.getSpikingColor()));
        NetworkGuiSettings.setZeroWeightColor(new Color(NetworkGuiPreferences.
                getZeroWeightColor()));
        NetworkGuiSettings.setMaxDiameter(NetworkGuiPreferences.getMaxDiameter());
        NetworkGuiSettings.setMinDiameter(NetworkGuiPreferences.getMinDiameter());
//        networkPanel.getRootNetwork().setTimeStep(NetworkPreferences.getTimeStep());
//        networkPanel.getRootNetwork().setPrecision(NetworkPreferences.getPrecision());
        NetworkGuiSettings.setNudgeAmount(NetworkGuiPreferences.getNudgeAmount());
        networkPanel.resetColors();
        setIndicatorColor();
        networkPanel.resetSynapseDiameters();
        fillFieldValues();
    }

    /**
     * Sets selected preferences as user defaults to be used each time program is launched.
     * Called when "ok" is pressed.
     */
    public void setAsDefault() {
        NetworkGuiPreferences.setBackgroundColor(NetworkGuiSettings.getBackgroundColor().getRGB());
        NetworkGuiPreferences.setLineColor(NetworkGuiSettings.getLineColor().getRGB());
        NetworkGuiPreferences.setHotColor(NetworkGuiSettings.getHotColor());
        NetworkGuiPreferences.setCoolColor(NetworkGuiSettings.getCoolColor());
        NetworkGuiPreferences.setExcitatoryColor(NetworkGuiSettings.getExcitatoryColor().getRGB());
        NetworkGuiPreferences.setInhibitoryColor(NetworkGuiSettings.getInhibitoryColor().getRGB());
//        NetworkPreferences.setLassoColor(SelectionMarquee.getMarqueeColor().getRGB());
//        NetworkPreferences.setSelectionColor(SelectionHandle.getSelectionColor().getRGB());
        NetworkGuiPreferences.setSpikingColor(NetworkGuiSettings.getSpikingColor().getRGB());
        NetworkGuiPreferences.setZeroWeightColor(NetworkGuiSettings.getZeroWeightColor().getRGB());
        NetworkGuiPreferences.setMaxDiameter(NetworkGuiSettings.getMaxDiameter());
        NetworkGuiPreferences.setMinDiameter(NetworkGuiSettings.getMinDiameter());
//        NetworkPreferences.setTimeStep(networkPanel.getRootNetwork().getTimeStep());
//        NetworkPreferences.setPrecision(networkPanel.getRootNetwork().getPrecision());
        NetworkGuiPreferences.setNudgeAmount(NetworkGuiSettings.getNudgeAmount());
    }

}
