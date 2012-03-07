package org.simbrain.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.genericframe.GenericJInternalFrame;

/**
 * An editor for beanshell scripts with syntax highlighting.  Based on RSyntaxTextArea
 * http://fifesoft.com/rsyntaxtextarea/
 * 
 * @author jeffyoshimi
 */
public class ScriptEditor extends GenericJInternalFrame {

	//TODO: Possibly pull JPanel out to a separate class.
	//TODO: Add save-as action
	
	/** Script directory. */
	private static final String SCRIPT_MENU_DIRECTORY = "."
			+ System.getProperty("file.separator") + "scripts"
			+ System.getProperty("file.separator") + "scriptmenu";

	/** Reference to file (for saving). */
	private File scriptFile;
	
	/** Main panel. */
	private JPanel mainPanel; 

	/** Main text area. */
	private final RSyntaxTextArea textArea;

	/**
	 * Construct the main frame.
	 */
	public ScriptEditor() {
		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		sp.setFoldIndicatorEnabled(true);
		createAttachMenuBar();

		mainPanel = new JPanel(new BorderLayout());
		mainPanel.add("Center", sp);
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		toolbar.add(getToolbarOpenClose());
		mainPanel.add("North", toolbar);

		setContentPane(mainPanel);
	}


	/**
	 * Creates the menu bar.
	 */
	private void createAttachMenuBar() {
		JMenuBar bar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenuItem openItem = new JMenuItem("Open...");
		openItem.setAction(getOpenScriptAction(this)); 		
		fileMenu.add(openItem);
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setAction(getSaveScriptAction(this));
		fileMenu.add(saveItem);
		fileMenu.addSeparator();
		JMenuItem closeItem = new JMenuItem("Close");
		closeItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose();
			}
		});
		fileMenu.add(closeItem);
		

		JMenu editMenu = new JMenu("Edit");
		JMenuItem preferences = new JMenuItem("Preferences...");
		editMenu.add(preferences);

		bar.add(fileMenu);
		bar.add(editMenu);
		setJMenuBar(bar);
	}

	/**
	 * Return a toolbar with buttons for opening from and saving to .bsh files.
	 * 
	 * @return the toolbar
	 */
	private JToolBar getToolbarOpenClose() {
		JToolBar toolbar = new JToolBar();
		toolbar.add(getOpenScriptAction(this));
		toolbar.add(getSaveScriptAction(this));
		return toolbar;
	}
	
	/**
	 * Returns the action for opening script files.
	 */
	private static Action getOpenScriptAction(final ScriptEditor frame) {
		return new AbstractAction() {

			// Initialize
			{
				putValue(SMALL_ICON, ResourceManager.getImageIcon("Open.png"));
				putValue(NAME, "Open Script (.bsh)...");
				putValue(SHORT_DESCRIPTION, "Open");
		        putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_O,
		                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			}

			@Override
			public void actionPerformed(ActionEvent e) {

				SFileChooser fileChooser = new SFileChooser(
						SCRIPT_MENU_DIRECTORY, "Edit Script", "bsh");
				frame.scriptFile = fileChooser.showOpenDialog();
				frame.setTitle(frame.scriptFile.getName());

				try {
					BufferedReader r = new BufferedReader(new FileReader(
							frame.scriptFile));
					frame.textArea.read(r, null);
					r.close();
					frame.textArea.setCaretPosition(0);
				} catch (IOException ioe) {
					ioe.printStackTrace();
					UIManager.getLookAndFeel().provideErrorFeedback(
							frame.textArea);
				}
			}


		};
	}
	
	/**
	 * Returns the action for saving script files.
	 */
	private static Action getSaveScriptAction(final ScriptEditor frame) {
		return new AbstractAction() {

			// Initialize
			{
				putValue(SMALL_ICON, ResourceManager.getImageIcon("Save.png"));
				putValue(SHORT_DESCRIPTION, "save");	
				putValue(Action.NAME, "Save");
		        putValue(this.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S,
		                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
			}
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (frame.scriptFile != null) {
					try {
						BufferedWriter r = new BufferedWriter(new FileWriter(
								frame.scriptFile));
						frame.textArea.write(r);
						r.close();
					} catch (IOException ioe) {
						ioe.printStackTrace();
						UIManager.getLookAndFeel().provideErrorFeedback(
								frame.textArea);
					}

				}
			}


		};
	}

}
