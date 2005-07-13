package org.simbrain.workspace;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;

import org.simbrain.network.NetworkFrame;
import org.simbrain.network.UserPreferences;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.world.dataworld.DataWorldFrame;
import org.simbrain.world.odorworld.OdorWorldFrame;



public class WorkspaceChangedDialog extends JDialog implements ActionListener {
	
	private LabelledItemPanel panel= new LabelledItemPanel();
	private ArrayList nCheckBoxList = new ArrayList();
	private ArrayList oCheckBoxList = new ArrayList();
	private ArrayList dCheckBoxList = new ArrayList();
	private ArrayList networkChangeList = new ArrayList();
	private ArrayList odorWorldChangeList = new ArrayList();
	private ArrayList dataWorldChangeList = new ArrayList();
	boolean finished = false;

	public WorkspaceChangedDialog(ArrayList __networkChangeList, ArrayList __odorWorldChangeList, ArrayList __dataWorldChangeList){
		networkChangeList = __networkChangeList;
		odorWorldChangeList = __odorWorldChangeList;
		dataWorldChangeList = __dataWorldChangeList;
		init();
	}
	
	public void init(){
		
		int x = 0;
		for (int i = 0; i < networkChangeList.size(); i++){
			NetworkFrame save = (NetworkFrame)networkChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			nCheckBoxList.add(x,checker);
		}
		int y = 0;
		for (int i = 0; i < odorWorldChangeList.size(); i++){
			OdorWorldFrame save = (OdorWorldFrame)odorWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			oCheckBoxList.add(y,checker);
		}
		int z = 0;
		for (int i = 0; i < dataWorldChangeList.size(); i++){
			DataWorldFrame save = (DataWorldFrame)dataWorldChangeList.get(i);
			JCheckBox checker = new JCheckBox();
			panel.addItem(save.getTitle(),checker);
			dCheckBoxList.add(z,checker);
		}
		
		
	JButton ok = new JButton("Save Checked Frames");
	panel.addItem("",ok);
	ok.addActionListener(this);
	ok.setActionCommand("ok");

	setContentPane(panel);
	setLocationRelativeTo(getParent());
	pack();
	setVisible(true);
	setTitle("Workspace has changed");
	setModal(true);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("ok")){
			for(int i = 0;i<nCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)nCheckBoxList.get(i);
				if(test.isSelected()){
					((NetworkFrame)networkChangeList.get(i)).getNetPanel().saveAs();
				}
			}
			for(int i = 0;i<oCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)oCheckBoxList.get(i);
				OdorWorldFrame testWorld = (OdorWorldFrame)odorWorldChangeList.get(i);
				if(test.isSelected()){
					testWorld.saveWorld(testWorld.getCurrentFile());
				}
			}
			for(int i = 0;i<dCheckBoxList.size();i++){
				JCheckBox test = (JCheckBox)dCheckBoxList.get(i);
				if(test.isSelected()){
					((DataWorldFrame)dataWorldChangeList.get(i)).saveWorld();
				}
			}
			dispose();
			quit();
		}
	}

	protected void quit() {
		UserPreferences.saveAll(); // Save all user preferences
		System.exit(0);
	}
}
