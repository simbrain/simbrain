/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.TransferHandler;

import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.interfaces.UpdateAction;
import org.simbrain.network.interfaces.UpdateManager.UpdateManagerListener;
import org.simbrain.network.update_actions.CustomUpdate;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ScriptEditor;
import org.simbrain.util.StandardDialog;

/**
 * Panel for display and ordering of network update actions.
 * 
 * @author jeff yoshimi
 *
 */
public class UpdateManagerPanel extends JPanel {

    /** The JList which represents current actions. */
    private final JList currentActionJList = new JList();
    
    /** The model object for current actions. */
    private final DefaultListModel currentActionListModel  = new DefaultListModel();

    /** The JList which represents available actions. */
    private final JList availableActionJList = new JList();

    /** The model object for current actions. */
    private final DefaultListModel availableActionListModel  = new DefaultListModel();

    /** Reference to root network. */
    private final RootNetwork network;
    
    /**
     * Creates a new action list panel.
     */
    public UpdateManagerPanel(final RootNetwork network) {

        super(new BorderLayout());
        this.network = network;

        // Set up Current Action list
        currentActionJList.setModel(currentActionListModel);
        updateCurrentActionsList();
        configureCurrentJList();
        currentActionJList.setDragEnabled(true);
        currentActionJList.setTransferHandler(createTransferHandler());
        currentActionJList.setDropMode(DropMode.INSERT);
        JScrollPane currentListScroll = new JScrollPane(currentActionJList);
        currentListScroll
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        currentListScroll
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        currentListScroll.setBorder(BorderFactory.createTitledBorder("Current Update Sequence"));
        //currentListScroll.setBackground(null);

        // Set up Available Action list
        availableActionJList.setModel(availableActionListModel);
        updateAvailableActionsList();
        configureAvailableJList();
        JScrollPane availableListScroll = new JScrollPane(availableActionJList);
        //availableActionJList.setBackground(getBackground());
        //availableListScroll.setBackground(null);
        
        availableListScroll
                .setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        availableListScroll
                .setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        availableListScroll.setBorder(BorderFactory.createTitledBorder("Unused Update Actions"));

        // Add lists
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                availableListScroll, currentListScroll);
        split.setResizeWeight(.5);
        add(split, BorderLayout.CENTER);

        
        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton customActionButton = new JButton(addCustomAction);       
        buttonPanel.add(customActionButton);
        JButton addActionsButton = new JButton(addActionsAction);       
        buttonPanel.add(addActionsButton);
        JButton deleteActionsButton = new JButton(deleteActionsAction);       
        buttonPanel.add(deleteActionsButton);
        updateAvailableActionsList();        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Listen for network updates
        network.getUpdateManager().addListener(listener);
        
        // TODO: Handle closing event
        // Should remove listener from update manager when this is closed
                
    }
    
    /**
     * Configure the JList.
     */
    private void configureCurrentJList() {
        currentActionJList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object updateAction, int index, boolean isSelected,
                    boolean cellHasFocus) {
                
                JLabel label = new JLabel((index + 1) + ": "
                        + ((UpdateAction) updateAction).getDescription());
				label.setToolTipText(((UpdateAction) updateAction)
						.getLongDescription());
                if (index == 0) {
                    label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                            Color.LIGHT_GRAY));                    
                } else {
                    label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                            Color.LIGHT_GRAY));                                        
                }
                label.setBackground(null);
                if (isSelected) {
                    label.setForeground(list.getSelectionForeground());
                    label.setBackground(list.getSelectionBackground());
                } else {
                    label.setForeground(list.getForeground());
                    label.setBackground(list.getBackground());
                }
                label.setEnabled(list.isEnabled());
                label.setFont(list.getFont());
                label.setOpaque(true);
                return label;
            }
        });
        currentActionJList.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		if (e.getClickCount() == 2) {
        			
        			// When double clicking on custom actions open an editor
					UpdateAction action = (UpdateAction) currentActionJList
							.getModel().getElementAt(
									currentActionJList.locationToIndex(e
											.getPoint()));
					if (action instanceof CustomUpdate) {
						ScriptEditor panel = new ScriptEditor(
								((CustomUpdate) action).getScriptString());
	        			StandardDialog dialog = ScriptEditor.getDialog(panel);
	                    dialog.pack();
	                    dialog.setLocationRelativeTo(null);
	                    dialog.setVisible(true);
	                    if (!dialog.hasUserCancelled()) {
	                    	((CustomUpdate)action).setScriptString(panel.getTextArea().getText());
	                    	((CustomUpdate)action).init();
	                    }            
					}        			
        		}
        		
        	}
        });
    }    
    
    
    /**
     * Configure the JList.
     */
    private void configureAvailableJList() {
        availableActionJList.setCellRenderer(new ListCellRenderer() {
            public Component getListCellRendererComponent(JList list,
                    Object updateAction, int index, boolean isSelected,
                    boolean cellHasFocus) {                
                JLabel label = new JLabel(((UpdateAction) updateAction).getDescription());
				label.setToolTipText(((UpdateAction) updateAction)
						.getLongDescription());
                if (index == 0) {
                    label.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0,
                            Color.LIGHT_GRAY));                    
                } else {
                    label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                            Color.LIGHT_GRAY));                                        
                }
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setForeground(list.getForeground());
                    label.setBackground(null);
                }
                label.setEnabled(list.isEnabled());
                label.setFont(list.getFont());
                label.setOpaque(true);
                return label;
            }
        });
        availableActionJList.addMouseListener(new MouseAdapter() {
        	public void mouseClicked(MouseEvent e) {
        		if (e.getClickCount() == 2) {
					UpdateAction action = (UpdateAction) availableActionJList
							.getModel().getElementAt(
									availableActionJList.locationToIndex(e
											.getPoint()));
					if (action instanceof CustomUpdate) {
						ScriptEditor panel = new ScriptEditor(
								((CustomUpdate) action).getScriptString());
	        			StandardDialog dialog = ScriptEditor.getDialog(panel);
	                    dialog.pack();
	                    dialog.setLocationRelativeTo(null);
	                    dialog.setVisible(true);
	                    if (!dialog.hasUserCancelled()) {
	                    	((CustomUpdate)action).setScriptString(panel.getTextArea().getText());
	                    	((CustomUpdate)action).init();
	                    }            
					}        			
        		}
        		
        	}
        });    
    }    
    
    
    /** Action which deletes selected actions. */
    Action deleteActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("minus.png"));
            putValue(NAME, "Remove selected action(s)");
            putValue(SHORT_DESCRIPTION, "Delete selected actions");
            UpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("BACK_SPACE"), this);
            UpdateManagerPanel.this.getInputMap(
                    JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                    KeyStroke.getKeyStroke("DELETE"), this);
            UpdateManagerPanel.this.getActionMap().put(this, this);
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : currentActionJList.getSelectedValues()) {
                network.getUpdateManager().removeAction((UpdateAction) action);
            }
        }
    };
    
    /** Action which allows for creation of custom action. */
    Action addCustomAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(NAME, "Custom action");
            putValue(SHORT_DESCRIPTION, "Add custom action");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
        	Scanner scanner = null;
        	File defaultScript = new File(System.getProperty("user.dir")
					+ "/etc/customNetworkUpdateTemplate.bsh");
        	StringBuilder scriptText = new StringBuilder();
        	String NL = System.getProperty("line.separator");
			try {
				scanner = new Scanner(new FileInputStream(defaultScript));
				while (scanner.hasNextLine()) {
					scriptText.append(scanner.nextLine() + NL);
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} finally {
				scanner.close();
			}
        	
			ScriptEditor panel = new ScriptEditor(scriptText.toString());
			panel.setScriptFile(defaultScript);
			StandardDialog dialog = ScriptEditor.getDialog(panel);
			// Setting script file to null prevents the template script from
			// being saved. Forces "save as"
			// if save button pressed.
			panel.setScriptFile(null); 
            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
            if (!dialog.hasUserCancelled()) {
            	CustomUpdate updateAction = new CustomUpdate(
						network, panel.getTextArea().getText());
            	network.getUpdateManager().addAction((UpdateAction) updateAction);
            }            
        	
        }
    };
    
    /** Action which deletes selected actions. */
    Action addActionsAction = new AbstractAction() {
        // Initialize
        {
            putValue(SMALL_ICON, ResourceManager.getImageIcon("plus.png"));
            putValue(NAME, "Add selected action(s)");
            putValue(SHORT_DESCRIPTION, "Add selected actions");
        }

        /**
         * {@inheritDoc}
         */
        public void actionPerformed(ActionEvent arg0) {
            for (Object action : availableActionJList.getSelectedValues()) {
                network.getUpdateManager().addAction((UpdateAction) action);
            }
        }
    };
    
    /**
     * Update the JList's model.
     */
    private void updateCurrentActionsList() {
        currentActionListModel.clear();
        for (UpdateAction action : network.getUpdateManager().getActionList()) {
            currentActionListModel.addElement(action);
        }
        repaint();
    }
    
    /**
     * Update the combo box showing potential actions.
     */
    private void updateAvailableActionsList() {
        availableActionListModel.clear();
        for (UpdateAction action : network.getUpdateManager().getAvailableActionList()) {
            availableActionListModel.addElement(action);
        }
        repaint();
    }
    
    /**
     * Listener for update manager changes.
     */
    private UpdateManagerListener listener = new UpdateManagerListener() {

        public void actionAdded(UpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionRemoved(UpdateAction action) {
            updateCurrentActionsList();
        }

        public void actionOrderChanged() {
            updateCurrentActionsList();
        }

        public void availableActionAdded(UpdateAction action) {
            updateAvailableActionsList();
        }

        public void availableActionRemoved(UpdateAction action) {
            updateAvailableActionsList();
        }
        
    };

    /**
     * Handle drag and drop events
     * @return
     */
    private TransferHandler createTransferHandler() {
        return new TransferHandler() {

            public boolean canImport(TransferHandler.TransferSupport info) {
//                if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
//                    return false;
//                }

                JList.DropLocation dl = (JList.DropLocation) info
                        .getDropLocation();
                if (dl.getIndex() == -1) {
                    return false;
                }
                return true;
            }

            public boolean importData(TransferHandler.TransferSupport info) {
                // if (!info.isDrop()) {
                // return false;
                // }

                JList.DropLocation dl = (JList.DropLocation) info
                        .getDropLocation();
                int targetIndex = dl.getIndex();
                int sourceIndex = currentActionJList.getSelectedIndex();
                int listSize = network.getUpdateManager().getActionList().size();
                if (targetIndex == listSize) {
                    targetIndex--;
                }
                if (sourceIndex == listSize) {
                    sourceIndex--;
                }                
                network.getUpdateManager().swapElements(sourceIndex, targetIndex);
                return true;
            }

            public int getSourceActions(JComponent c) {
                return TransferHandler.MOVE;
            }

            protected Transferable createTransferable(JComponent c) {
                return new StringSelection("");
            }

        };
    }


//    /**
//     * Test panel.
//     */
//    public static void main(String[] args) {
//        JFrame frame = new JFrame();
//        List<String> actions = new ArrayList<String>();
//        actions.add("Buffered Update");
//        actions.add("Group 1");
//        actions.add("Group 2");
//        actions.add("Neuron 1");
//        UpdateManagerPanel panel = new UpdateManagerPanel(frame, actions);
//        frame.setContentPane(panel);
//        frame.setVisible(true);
//        frame.pack();
//    }

}
