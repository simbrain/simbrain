package org.simbrain.world.odorworld;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

/**
 * 
 * <b>OdorWorldFrameMenu</b>
 */
public class OdorWorldFrameMenu extends JMenuBar implements MenuListener{

	private OdorWorldFrame parentFrame;
	
	public JMenu fileMenu = new JMenu("File  ");
	public JMenuItem saveItem = new JMenuItem("Save");
	public JMenuItem saveAsItem = new JMenuItem("Save As");
	public JMenuItem openItem = new JMenuItem("Open world");
	public JMenuItem prefsItem = new JMenuItem("World preferences");
	public JMenuItem close = new JMenuItem("Close");

	public JMenu editMenu = new JMenu("Edit  ");
	public JMenuItem copyItem = new JMenuItem("Copy");
	public JMenuItem cutItem = new JMenuItem("Cut");
	public JMenuItem pasteItem = new JMenuItem("Paste");
	public JMenuItem clearAllItem = new JMenuItem("Clear all entities");
	
	public JMenu scriptMenu = new JMenu("Script ");
	public JMenuItem scriptItem = new JMenuItem("Open script dialog");
	
	public JMenu helpMenu = new JMenu("Help");
	public JMenuItem helpItem = new JMenuItem("World Help");


	
	public OdorWorldFrameMenu(OdorWorldFrame frame){
		parentFrame = frame;
	}

	
	public void setUpMenus(){
		parentFrame.setJMenuBar(this);
		
		setUpFileMenu();
		
		setUpEditMenu();
		
		add(scriptMenu);
		scriptMenu.add(scriptItem);
		scriptItem.addActionListener(parentFrame);
		
		add(helpMenu);
		helpMenu.add(helpItem);
		helpItem.addActionListener(parentFrame);

	}

	
	public void setUpFileMenu(){
		add(fileMenu);
		fileMenu.add(openItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAsItem);
		fileMenu.addSeparator();
		fileMenu.add(prefsItem);
		fileMenu.add(close);
		fileMenu.addMenuListener(this);

		close.addActionListener(parentFrame);
		close.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		saveItem.addActionListener(parentFrame);
		saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		saveAsItem.addActionListener(parentFrame);
		openItem.addActionListener(parentFrame);
		openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		prefsItem.addActionListener(parentFrame);
		
	}
	
	public void setUpEditMenu(){
		add(editMenu);
		
		editMenu.add(cutItem);
		editMenu.add(copyItem);
		editMenu.add(pasteItem);
		editMenu.addSeparator();
		editMenu.addSeparator();
		editMenu.add(clearAllItem);
		
		cutItem.addActionListener(parentFrame.getWorld());
		cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		copyItem.addActionListener(parentFrame.getWorld());
		copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		pasteItem.addActionListener(parentFrame.getWorld());
		pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		clearAllItem.addActionListener(parentFrame.getWorld());
	}


	public void menuSelected(MenuEvent e) {
		if(e.getSource().equals(fileMenu)){
			if(parentFrame.isChangedSinceLastSave()){
				saveItem.setEnabled(true);
			} else if (!parentFrame.isChangedSinceLastSave()){
				saveItem.setEnabled(false);
			}
		}
	}

	public void menuDeselected(MenuEvent arg0) {
	}


	public void menuCanceled(MenuEvent arg0) {
	}
}