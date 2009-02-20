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
//        networkPanel.setBackgroundColor(new Color(NetworkPreferences.getBackgroundColor()));
          NetworkGuiSettings.setLineColor(new Color(NetworkGuiPreferences.getLineColor()));
//        networkPanel.setHotColor(NetworkPreferences.getHotColor());
//        networkPanel.setCoolColor(NetworkPreferences.getCoolColor());
//        networkPanel.setExcitatoryColor(new Color(NetworkPreferences.getExcitatoryColor()));
//        networkPanel.setInhibitoryColor(new Color(NetworkPreferences.getInhibitoryColor()));
//        SelectionMarquee.setMarqueeColor(new Color(NetworkPreferences.getLassoColor()));
//        SelectionHandle.setSelectionColor(new Color(NetworkPreferences.getSelectionColor()));
//        networkPanel.setSpikingColor(new Color(NetworkPreferences.getSpikingColor()));
//        networkPanel.setZeroWeightColor(new Color(NetworkPreferences.getZeroWeightColor()));
//        networkPanel.setMaxDiameter(NetworkPreferences.getMaxDiameter());
//        networkPanel.setMinDiameter(NetworkPreferences.getMinDiameter());
//        networkPanel.getRootNetwork().setTimeStep(NetworkPreferences.getTimeStep());
//        networkPanel.getRootNetwork().setPrecision(NetworkPreferences.getPrecision());
//        networkPanel.setNudgeAmount(NetworkPreferences.getNudgeAmount());
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
//        NetworkPreferences.setBackgroundColor(networkPanel.getBackground().getRGB());
            NetworkGuiPreferences.setLineColor(NetworkGuiSettings.getLineColor().getRGB());
//        NetworkPreferences.setHotColor(networkPanel.getHotColor());
//        NetworkPreferences.setCoolColor(networkPanel.getCoolColor());
//        NetworkPreferences.setExcitatoryColor(networkPanel.getExcitatoryColor().getRGB());
//        NetworkPreferences.setInhibitoryColor(networkPanel.getInhibitoryColor().getRGB());
//        NetworkPreferences.setLassoColor(SelectionMarquee.getMarqueeColor().getRGB());
//        NetworkPreferences.setSelectionColor(SelectionHandle.getSelectionColor().getRGB());
//        NetworkPreferences.setSpikingColor(networkPanel.getSpikingColor().getRGB());
//        NetworkPreferences.setZeroWeightColor(networkPanel.getZeroWeightColor().getRGB());
//        NetworkPreferences.setMaxDiameter(networkPanel.getMaxDiameter());
//        NetworkPreferences.setMinDiameter(networkPanel.getMinDiameter());
//        NetworkPreferences.setTimeStep(networkPanel.getRootNetwork().getTimeStep());
//        NetworkPreferences.setPrecision(networkPanel.getRootNetwork().getPrecision());
//        NetworkPreferences.setNudgeAmount(networkPanel.getNudgeAmount());
    }

}
