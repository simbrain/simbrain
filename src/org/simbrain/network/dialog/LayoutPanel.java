/*
 * Created on Oct 5, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Kyle Baron
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

package org.simbrain.network.dialog;

import javax.swing.*;

import org.simbrain.util.LabelledItemPanel;

public class LayoutPanel extends LabelledItemPanel {
	public static final String LINE = "Line";
	public static final String GRID = "Grid";
	
	public static String[] layoutList = {LINE, GRID};
	
	private JComboBox cbLayouts = new JComboBox(layoutList);
	private JTextField tfRows = new JTextField();
	private JTextField tfColumns = new JTextField();
	
	public LayoutPanel(){
		
		this.addItem("Layout Style", cbLayouts);
	}

	public String getCurrentLayout(){
		return cbLayouts.getSelectedItem().toString();
	}
	
	public void setCurrentLayout(String layout){
		if(layout.equalsIgnoreCase(LINE)){
			cbLayouts.setSelectedIndex(0);
		}else if(layout.equalsIgnoreCase(GRID)){
			cbLayouts.setSelectedIndex(1);
		}
	}
	
	public void initPanel(){
		
		if(getCurrentLayout().equalsIgnoreCase(GRID)){
			this.addItem("Rows", tfRows);
			this.addItem("Columns", tfColumns);
		}
	}
}
