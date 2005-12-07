package org.simbrain.world.visionworld;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.workspace.Workspace;

/**
* <b>VisionWorldFrame</b> provides the internal frame that contains
* the VisionWorld neural interface.
*
* @author RJB
*/
public class VisionWorldFrame extends JInternalFrame implements ComponentListener,  ActionListener {

    /**
    * The world contained in this frame.
    */
    private VisionWorld world;
//    private static final String FS = "/"; //System.getProperty("file.separator");Separator();
//    private File current_file = null;
//    private String currentDirectory = VisionWorldPreferences.getCurrentDirectory();

    /**
    * The workspace that contains this frame.
    */
    private Workspace workspace;

    // For workspace persistence
    /**
    * Path for Persistence.
    */
    private String path;

    /**
    * xpos for Persistence.
    */
    private int xpos;

    /**
    * ypos for Persistence.
    */
    private int ypos;

    /**
    * the_width for Persistence.
    */
    private int the_width;

    /**
    * the_height for Persistence.
    */
    private int the_height;


    /**
     * Constructor to attach this to a workspace.
     * @param ws the workspace that is this frame's parent
     */
    public VisionWorldFrame(final Workspace ws) {
        this.workspace = ws;

        world = new VisionWorld(this);

        this.setResizable(true);
        this.setMaximizable(true);
        this.setIconifiable(true);
        this.setClosable(true);
        this.addComponentListener(this);
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add("Center",  world);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton send = new JButton("Send");
        send.addActionListener(this);
        send.setActionCommand("Send");
        buttonPanel.add(send);
        this.getContentPane().add("South",  buttonPanel);
        this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);

        createMenus();

        setVisible(true);

    }

    /**
     * Creates and sets the menu for this frame.
     */
    private void createMenus() {
        JMenuBar menuBar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenu settings = new JMenu("Settings");
        JMenu help = new JMenu("Help");
        JMenuItem settingsItem = new JMenuItem("Pixels");
        JMenuItem helpItem = new JMenuItem("Help");
        settingsItem.addActionListener(this);
        settingsItem.setActionCommand("pixels");
        helpItem.addActionListener(this);
        helpItem.setActionCommand("help");
        settings.add(settingsItem);
        help.add(helpItem);
        menuBar.add(file);
        menuBar.add(settings);
        menuBar.add(help);
        this.setJMenuBar(menuBar);
    }

    /**
     * @return the world this frame contains
     */
    public VisionWorld getWorld() {
        return world;
    }


    /**
     * Ensures that the world keeps up with the frame resizing.
     * @param e the ComponentEvent triggering this method
     */
    public void componentResized(final ComponentEvent e) {
        world.rebuild();
        pack();
    }

    /**
     * @param e the ComponentEvent triggering this method
     */
    public void componentMoved(final ComponentEvent e) {
    }

    /**
     * @param e the ComponentEvent triggering this method
     */
    public void componentShown(final ComponentEvent e) {
    }

    /**
     * @param e the ComponentEvent triggering this method
     */
    public void componentHidden(final ComponentEvent e) {
    }

    /**
     * Handles button and menu firing events.
     * @param e the ActionEvent triggering this method
     */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("Send")) {
            send();
        } else if (e.getActionCommand().equals("pixels")) {
            showPixelDialog();
        } else if (e.getActionCommand().equals("help")) {
            showHelp();
        }
    }

    /**
     * Shows the help window in the system default browser.
     */
    private void showHelp() {
    }

    /**
     * Shows a dialog for altering the dimensions of the pixel set in the world.
     */
    private void showPixelDialog() {
        final JDialog dia = new JDialog(workspace, "Pixel Settings");
        dia.setLayout(new BorderLayout());
        dia.setModal(true);
        dia.setLocationRelativeTo(this);
        final JTextField width = new JTextField(world.getNumPixelsRow());
        final JTextField height = new JTextField(world.getNumPixelsColumn());
        LabelledItemPanel pan = new LabelledItemPanel();
        pan.addItem("Width (in pixels)", width);
        pan.addItem("Height(in pixels)", height);
        dia.getContentPane().add(pan, BorderLayout.CENTER);
        ActionListener list = new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (((JButton) e.getSource()).getText().equals("OK")) {
                    int w = 0, h = 0;
                    try {
                        w = Integer.parseInt(width.getText());
                    } catch (NumberFormatException e1) {
                        width.grabFocus();
                        width.selectAll();
                    }
                    try {
                        h = Integer.parseInt(height.getText());
                    } catch (NumberFormatException e1) {
                        height.grabFocus();
                        height.selectAll();
                    }
                    if (w != 0 && h != 0) {
                        world.redimension(w, h);
                        world.rebuild();
                        dia.setVisible(false);
                    }
                } else if (((JButton) e.getSource()).getText().equals("Cancel")) {
                    dia.setVisible(false);
                }
            }
        };

        JPanel buttPan = new JPanel();
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(list);
        cancel.addActionListener(list);
        buttPan.add(ok);
        buttPan.add(cancel);
        dia.getContentPane().add(buttPan, BorderLayout.PAGE_END);
        dia.pack();
        dia.setVisible(true);
    }

    /**
     * Sends the current formation of VisionWorld to the attached networks (calls a like-named method in VisionWorld).
     */
    private void send() {
    }


    //filehandling methods

}
