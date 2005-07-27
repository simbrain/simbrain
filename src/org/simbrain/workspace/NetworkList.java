/*
 * Created on Jul 27, 2005
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.simbrain.workspace;

import java.awt.Color;
import java.util.ArrayList;

import org.simbrain.coupling.Coupling;
import org.simbrain.network.NetworkFrame;
import org.simbrain.network.UserPreferences;

/**
 * @author yoshimi
 *
 * Methods that apply to all workspace networks are handled here.
 */
public class NetworkList extends ArrayList {
	
	/**
	 * Repaint all open network panels.  Useful when workspace changes happen 
	 * that need to be broadcast; also essential when default workspace is initially
	 * opened.
	 */
	public void repaintAllNetworkPanels() {
		
		for(int j = 0; j < this.size(); j++) {
			NetworkFrame net = (NetworkFrame)this.get(j);
			net.getNetPanel().repaint();
		}
		
	}
	
	/**
	 * Update background colors of all networks
	 */
	public void updateBackgrounds(Color theColor) {
		
		for(int j = 0; j < this.size(); j++) {
			NetworkFrame net = (NetworkFrame)this.get(j);
			net.getNetPanel().setBackground(theColor);
		}
		
	}
	
	/**
	 * Restore all User Preference-base network properties to their default values
	 *
	 */
	public void restoreDefaults() {
		updateBackgrounds(new Color(UserPreferences.getBackgroundColor()));
	}
	
	/**
	 * @return a list of networks which have changed since last save
	 */
	public ArrayList getChanges(){
		ArrayList ret = new ArrayList();

		for ( int i = 0; i < size();i++){
			NetworkFrame test = (NetworkFrame)get(i);
			if (test.isChangedSinceLastSave()){
				ret.add(test);
			}
		}
 
		return ret;
		
	}
}
