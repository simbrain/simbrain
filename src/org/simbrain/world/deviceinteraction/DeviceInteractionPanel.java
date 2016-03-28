package org.simbrain.world.deviceinteraction;


import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * @author Amanda Pandey
 */
public class DeviceInteractionPanel extends JPanel {
    private final KeyboardWorld world;

    /** Text area for inputting text into networks. */
    private JTextArea textArea = new JTextArea();

    /**
     * Toolbar for opening and closing the world. Must be defined at component
     * level.
     */
    private JToolBar openCloseToolBar = null;

    /**
     * Construct a reader panel to represent data in a text world.
     *
     * @param theWorld the world
     * @param toolbar pass in open / close toolbar
     */
    public DeviceInteractionPanel(KeyboardWorld theWorld, JToolBar toolbar) {
        this.world = theWorld;
        this.openCloseToolBar = toolbar;

        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

        // Set up toolbar
        JPanel topToolbarPanel = new JPanel();
        topToolbarPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        if (openCloseToolBar != null) {
            topToolbarPanel.add(openCloseToolBar);
        }
        JToolBar dictionaryToolBar = new JToolBar();
        dictionaryToolBar
                .add(new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //TODO
                    }
                });
        topToolbarPanel.add(dictionaryToolBar);
        add(topToolbarPanel, BorderLayout.NORTH);

        // Force a bit of room at bottom
        add(new JLabel("  "), BorderLayout.SOUTH);

        // Set up main text area
        textArea.setLineWrap(true);
        textArea.setText(world.getText());

        // Reset text position when user clicks in text area
        textArea.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                world.setPosition(textArea.getCaretPosition(), false);
            }

        });

        // Listener for changes in the textarea (i.e. adding or removing text
        // directly in the area).
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            public void changedUpdate(DocumentEvent arg0) {
                //TODO
            }

            public void insertUpdate(DocumentEvent arg0) {
                //TODO
            }

            public void removeUpdate(DocumentEvent arg0) {
                //TODO
            }

        });

        final JScrollPane inputScrollPane = new JScrollPane(textArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(inputScrollPane, BorderLayout.CENTER);

        world.addKeyboardListener(new KeyboardWorld.KeyboardListener() {

            @Override
            public void keyPressed(Character key) {
                textArea.setText(key.toString());
                if (world.getPosition() < textArea.getDocument().getLength()) {
                    textArea.setCaretPosition(world.getPosition());
                }
            }

            @Override
            public void positionChanged(int position) {
                textArea.setCaretPosition(world.getPosition());
            }
        });

    }

    /**
     * @return the world
     */
    public KeyboardWorld getWorld() {
        return world;
    }

}
