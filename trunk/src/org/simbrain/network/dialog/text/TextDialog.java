package org.simbrain.network.dialog.text;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
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
import javax.swing.text.Document;

import org.simbrain.network.nodes.TextObject;
import org.simbrain.util.StandardDialog;

import edu.umd.cs.piccolox.nodes.PStyledText;

public class TextDialog extends StandardDialog implements ActionListener, ListSelectionListener {

    /** Selection list. */
    private ArrayList<TextObject> selectionList = new ArrayList<TextObject>();

    /** Text objects being modified. */
    private ArrayList<PStyledText> textObjectList;

    /** Tabbed pane for font and color effects. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Color Chooser. */
    private JColorChooser colorTab = new JColorChooser();

    /** Gets available fonts. */
    private static String[] fonts = GraphicsEnvironment
            .getLocalGraphicsEnvironment().getAvailableFontFamilyNames();

    /** Font style list. */
    private static String[] style = {"Regular", "Bold", "Italic",
            "Bold Italic"};

    /** Font size list. */
    private static String[] size = {"8", "9", "10", "11", "12", "14", "16",
            "18", "20", "22", "24", "26", "28", "36", "48", "72"};

    /** Font. */
    private Font font;

    /** Font text type. */
    private String textType;

    /** Font text Style. */
    private int textStyle;

    /** Font text size. */
    private int textSize;

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
    Container container = getContentPane();
    JPanel panel = new JPanel();

    public TextDialog(final ArrayList<TextObject> selectedText) {
        selectionList = selectedText;
//        setTextObjectSelection();
        setFont(new Font("Courier New", Font.PLAIN, 12));
        init();
        fillFieldValues();
    }

    /**
     * Get the text settings.
     */
    private void setTextObjectSelection() {
        textObjectList.clear();

        for (TextObject t : selectionList) {
            textObjectList.add(t.getPtext());
        }
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

//        tabbedPane.addTab("Font", container);
//        tabbedPane.addTab("Color", colorTab);
//        setContentPane(tabbedPane);
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

        textType = font.getFontName();
        textStyle = font.getStyle();
        textSize = font.getSize();

        jtfTest.setFont(new Font(textType, textStyle, textSize));

        for (int i = 0; i < fList.getModel().getSize(); i++) {
            fList.setSelectedIndex(i);

            if (font.getName().equals((String) fList.getSelectedValue())) {
                found = true;
                setScrollPos(jspFont, fList, i);

                break;
            }
        }

        if (!found) {
            fList.clearSelection();
        }

        stList.setSelectedIndex(font.getStyle());

        found = false;

        for (int i = 0; i < sizeList.getModel().getSize(); i++) {
            sizeList.setSelectedIndex(i);

            if (font.getSize() == Integer.parseInt((String) sizeList
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

    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Sets the current font of the font chooser.
     *
     * @param aFont font to be set.
     */
    public void setFont(final Font aFont) {
        font = aFont;
    }

    /**
     * Gets the current font of the font chooser.
     *
     * @return the current font chooser.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Gets the name of the font chooser's current font.
     *
     * @return the name of the font chooser's current font
     */
    public String getFontName() {
        return font.getFontName();
    }

    /**
     * Gets the style of the font chooser's current font.
     *
     * @return current font
     */
    public int getFontStyle() {
        return font.getStyle();
    }

    /**
     * Gets the size of the font chooser's current font.
     *
     * @return font size
     */
    public int getFontSize() {
        return font.getSize();
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
                if (jtfSize.getText().trim().equals(
                        (String) sizeList.getModel().getElementAt(i))) {
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
            } else if (jtfStyle.getText().equals("Bold")) {
                textStyle = 1;
            } else if (jtfStyle.getText().equals("Italic")) {
                textStyle = 2;
            } else if (jtfStyle.getText().equals("Bold Italic")) {
                textStyle = 3;
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
     * Takes a scrollPane, a JList and an index in the JList and sets the scrollPane's
     * scrollbar so that the selected item in the JList is in about the middle of the
     * scrollPane.
     * @param sp scroll pane
     * @param list list of items
     * @param index of item
     */
    private void setScrollPos(final JScrollPane sp, final JList list, final int index) {
        int unitSize = sp.getVerticalScrollBar().getMaximum()
                / list.getModel().getSize();

        sp.getVerticalScrollBar().setValue((index - 2) * unitSize);
    }

    public void commitChanges() {
        for (int i = 0; i < textObjectList.size(); i++) {
            PStyledText textRef = textObjectList.get(i);
//            textRef.getParent().set
        }
    }
}
