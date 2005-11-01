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

// Need javadoc
public class VisionWorldFrame extends JInternalFrame implements ComponentListener,  ActionListener {

    private VisionWorld world;
//    private static final String FS = "/"; //System.getProperty("file.separator");Separator();
//    private File current_file = null;
//    private String currentDirectory = VisionWorldPreferences.getCurrentDirectory();
    private Workspace workspace;

    // For workspace persistence
    private String path;
    private int xpos;
    private int ypos;
    private int the_width;
    private int the_height;


    //Loader methods for visionworld
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

    public VisionWorld getWorld() {
        return world;
    }


    public void componentResized(final ComponentEvent arg0) {
        world.rebuild();
        pack();
    }

    public void componentMoved(final ComponentEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void componentShown(final ComponentEvent arg0) {
        // TODO Auto-generated method stub

    }

    public void componentHidden(final ComponentEvent e) {
        // TODO Auto-generated method stub

    }

    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("Send")) {
            send();
        } else if (e.getActionCommand().equals("pixels")) {
            showPixelDialog();
        } else if (e.getActionCommand().equals("help")) {
            showHelp();
        }
    }



    private void showHelp() {
    }

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



    private void send() {
    }







    //filehandling methods

}
