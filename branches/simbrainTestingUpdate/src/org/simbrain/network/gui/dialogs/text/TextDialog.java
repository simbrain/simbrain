package org.simbrain.network.gui.dialogs.text;

import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.simbrain.network.gui.nodes.TextNode;
import org.simbrain.util.StandardDialog;

/**
 * Font dialog. Adapted from a post on website from a long time ago...
 *
 * @author jyoshimi
 */
public class TextDialog extends StandardDialog implements ActionListener,
        ListSelectionListener {

    /** Selection list. */
    private ArrayList<TextNode> selectionList = new ArrayList<TextNode>();

    /** Tabbed pane for font and color effects. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Color Chooser. */
    private JColorChooser colorTab = new JColorChooser();

    /** Gets available fonts. */
    private static String[] fonts = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

    /** Font style list. */
    private static String[] style = { "Regular", "Bold", "Italic",
            "Bold Italic" };

    /** Font size list. */
    private static String[] size = { "8", "9", "10", "11", "12", "14", "16",
            "18", "20", "22", "24", "26", "28", "36", "48", "72" };

    /** Font text type. */
    private String textType;

    /** Font text Style. */
    private int textStyle;

    /** Font text size. */
    private int textSize;

    /** Is font italic. */
    private boolean italic;

    /** Is font bold. */
    private boolean bold;

    /** Font list. */
    private JList fList = new JList(fonts);

    /** Style list. */
    private JList stList = new JList(style);

    /** Size list. */
    private JList sizeList = new JList(size);

    /** Font list text field. */
    private JTextField jtfFonts = new JTextField();

    /** Font style text field. */
    private JTextField jtfStyle = new JTextField();

    /** Font size text field. */
    private JTextField jtfSize = new JTextField();

    /** Font label. */
    private JLabel jlbFonts = new JLabel("Font:");

    /** Style label. */
    private JLabel jlbStyle = new JLabel("Style:");

    /** Size label. */
    private JLabel jlbSize = new JLabel("Size:");

    /** Font scroll pane. */
    private JScrollPane jspFont = new JScrollPane(fList);

    /** Style scroll pane. */
    private JScrollPane jspStyle = new JScrollPane(stList);

    /** Size scroll pane. */
    private JScrollPane jspSize = new JScrollPane(sizeList);

    /** Text field for selected font. */
    private JTextField jtfTest = new JTextField("AaBbYyZz");

    /** Main container. */
    Container container = getContentPane();

    /** Main panel. */
    JPanel panel = new JPanel();

    /**
     * Construct text dialog.
     *
     * @param selectedTextNodes currently selected text nodes in the network
     *            panel.
     */
    public TextDialog(final ArrayList<TextNode> selectedTextNodes) {
        selectionList = selectedTextNodes;
        init();
        fillFieldValues();
    }

    /**
     * Initializes font chooser dialog.
     */
    public void init() {
        super.setTitle("Font Chooser");
        TitledBorder panelBorder = new TitledBorder("Sample");
        fList.setSelectionMode(0);
        stList.setSelectionMode(0);
        sizeList.setSelectionMode(0);
        jtfTest.setHorizontalAlignment(JTextField.CENTER);
        jspFont.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jspStyle.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jspSize.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.setBorder(panelBorder);

        jtfFonts.setBounds(8, 5, 121, 20);
        jspFont.setBounds(8, 29, 121, 82);

        jtfStyle.setBounds(136, 5, 121, 20);
        jspStyle.setBounds(136, 29, 121, 82);

        jtfSize.setBounds(264, 5, 41, 20);
        jspSize.setBounds(264, 29, 41, 82);

        panel.setBounds(6, 121, 301, 67);

        container.add(jlbFonts);
        container.add(jtfFonts);
        container.add(jspFont);

        container.add(jlbStyle);
        container.add(jtfStyle);
        container.add(jspStyle);

        container.add(jlbSize);
        container.add(jtfSize);
        container.add(jspSize);

        container.add(panel);

        jtfTest.setBounds(8, 20, 288, 35);

        panel.add(jtfTest);

        container.setLayout(null);
        panel.setLayout(null);

        setSize(340, 278);
        setResizable(false);
        setModal(true);

        // tabbedPane.addTab("Font", container);
        // tabbedPane.addTab("Color", colorTab);
        // setContentPane(tabbedPane);
        setContentPane(container);

        jtfFonts.addActionListener(this);
        jtfSize.addActionListener(this);
        jtfStyle.addActionListener(this);
        fList.addListSelectionListener(this);
        stList.addListSelectionListener(this);
        sizeList.addListSelectionListener(this);
    }

    /**
     * Sets the fields to the current values.
     */
    private void fillFieldValues() {

        boolean found = false;

        TextNode firstNode = selectionList.get(0);

        String theTextType = firstNode.getTextObject().getFontName();
        int theTextSize = firstNode.getTextObject().getFontSize();
        int theTextStyle = 0;
        if (firstNode.getTextObject().isBold()) {
            theTextStyle = Font.BOLD;
        }
        if (firstNode.getTextObject().isItalic()) {
            theTextStyle += Font.ITALIC;
        }

        jtfTest.setFont(new Font(textType, textStyle, textSize));

        for (int i = 0; i < fList.getModel().getSize(); i++) {
            fList.setSelectedIndex(i);
            if (theTextType.equalsIgnoreCase((String) fList.getSelectedValue())) {
                found = true;
                setScrollPos(jspFont, fList, i);
                break;
            }
        }

        if (!found) {
            fList.clearSelection();
        }

        stList.setSelectedIndex(theTextStyle);

        found = false;

        for (int i = 0; i < sizeList.getModel().getSize(); i++) {
            sizeList.setSelectedIndex(i);
            if (theTextSize == Integer.parseInt((String) sizeList
                    .getSelectedValue())) {
                found = true;
                setScrollPos(jspSize, sizeList, i);

                break;
            }
        }

        if (!found) {
            sizeList.clearSelection();
        }
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * @see AbstractAction.
     */
    public void actionPerformed(final ActionEvent e) {
        boolean found = false;
        if (e.getSource() == jtfFonts) {
            textType = jtfFonts.getText();

            for (int i = 0; i < fList.getModel().getSize(); i++) {
                if (((String) fList.getModel().getElementAt(i))
                        .startsWith(jtfFonts.getText().trim())) {
                    fList.setSelectedIndex(i);
                    setScrollPos(jspFont, fList, i);
                    found = true;

                    break;
                }
            }

            if (!found) {
                fList.clearSelection();
            } else {
                jtfTest.setFont(new Font(textType, textStyle, textSize));
            }

            found = false;
        } else if (e.getSource() == jtfSize) {
            textSize = (Integer.parseInt(jtfSize.getText().trim()));
            jtfTest.setFont(new Font(textType, textStyle, textSize));

            for (int i = 0; i < sizeList.getModel().getSize(); i++) {
                if (jtfSize.getText().trim()
                        .equals(sizeList.getModel().getElementAt(i))) {
                    sizeList.setSelectedIndex(i);
                    setScrollPos(jspSize, sizeList, i);
                    found = true;

                    break;
                }
            }

            if (!found) {
                sizeList.clearSelection();
            }

            found = false;
        } else if (e.getSource() == jtfStyle) {
            if (jtfStyle.getText().equals("Regular")) {
                textStyle = Font.PLAIN;
            } else if (jtfStyle.getText().equals("Bold")) {
                textStyle = Font.BOLD;
            } else if (jtfStyle.getText().equals("Italic")) {
                textStyle = Font.ITALIC;
            } else if (jtfStyle.getText().equals("Bold Italic")) {
                textStyle = Font.BOLD & Font.ITALIC;
            }

            stList.setSelectedIndex(textStyle);

            jtfTest.setFont(new Font(textType, textStyle, textSize));
        }
    }

    /**
     * @see AbstractAction.
     */
    public void valueChanged(final ListSelectionEvent e) {
        if (e.getSource() == fList) {
            if (fList.getSelectedValue() != null) {
                jtfFonts.setText(((String) (fList.getSelectedValue())));
            }

            textType = jtfFonts.getText();
            jtfTest.setFont(new Font(textType, textStyle, textSize));
        } else if (e.getSource() == stList) {
            jtfStyle.setText(((String) (stList.getSelectedValue())));

            if (jtfStyle.getText().equals("Regular")) {
                textStyle = 0;
                italic = false;
                bold = false;
            } else if (jtfStyle.getText().equals("Bold")) {
                textStyle = 1;
                italic = false;
                bold = true;
            } else if (jtfStyle.getText().equals("Italic")) {
                textStyle = 2;
                italic = true;
                bold = false;
            } else if (jtfStyle.getText().equals("Bold Italic")) {
                textStyle = 3;
                italic = true;
                bold = true;
            }

            jtfTest.setFont(new Font(textType, textStyle, textSize));
        } else if (e.getSource() == sizeList) {
            if (sizeList.getSelectedValue() != null) {
                jtfSize.setText(((String) (sizeList.getSelectedValue())));
            }

            textSize = (Integer.parseInt(jtfSize.getText().trim()));
            jtfTest.setFont(new Font(textType, textStyle, textSize));
        }
    }

    /**
     * Takes a scrollPane, a JList and an index in the JList and sets the
     * scrollPane's scrollbar so that the selected item in the JList is in about
     * the middle of the scrollPane.
     *
     * @param sp scroll pane
     * @param list list of items
     * @param index of item
     */
    private void setScrollPos(final JScrollPane sp, final JList list,
            final int index) {
        int unitSize = sp.getVerticalScrollBar().getMaximum()
                / list.getModel().getSize();

        sp.getVerticalScrollBar().setValue((index - 2) * unitSize);
    }

    /**
     * Commit changes to underlying nodes.
     */
    private void commitChanges() {
        for (TextNode node : selectionList) {
            node.getTextObject().setFontName(textType);
            node.getTextObject().setFontSize(textSize);
            node.getTextObject().setItalic(italic);
            node.getTextObject().setBold(bold);
            node.update();
        }
    }
}
