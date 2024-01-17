package org.simbrain.util.widgets;


import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.simbrain.util.LabelledItemPanel;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * A find / replace dialog for RSyntaxTextArea (by Robert Futrell).
 * <p>
 * See http://fifesoft.com/rsyntaxtextarea/
 *
 * @author Jeff Yoshimi
 */
public class FindReplaceDialog extends JPanel {

    /**
     * Search field.
     */
    private JTextField searchField;

    /**
     * Replace field.
     */
    private JTextField replaceField;

    /**
     * Whether to use regular expressions.
     */
    private JCheckBox regexCB = new JCheckBox("Regular expressions");

    /**
     * Match case.
     */
    private JCheckBox matchCaseCB = new JCheckBox("Match Case");

    /**
     * Whole world.
     */
    private JCheckBox wholeWordCB = new JCheckBox("Whole word");

    /**
     * Wrap search.
     */
    private JCheckBox wrapSearchCB = new JCheckBox("Wrap Search");

    /**
     * Search backward.
     */
    private JRadioButton backwardSearch = new JRadioButton("Backward");

    /**
     * Search forward.
     */
    private JRadioButton forwardSearch = new JRadioButton("Forward");

    /**
     * Reference to RSyntaxTextArea.
     */
    private RSyntaxTextArea textArea;

    // TODO
    // Say how may replacements were made in replace all
    // Various warnings, etc.
    public FindReplaceDialog(final JFrame frame, final SimbrainTextArea textArea) {
        setLayout(new GridLayout(4, 1));
        this.textArea = textArea;

        // Find replace Panel
        LabelledItemPanel findReplacePanel = new LabelledItemPanel();

        // Options Panel
        JPanel optionsPanel = new JPanel(new GridLayout(2, 2));
        Border paddingBorderOptions = new EmptyBorder(10, 10, 10, 10);
        Border titleBorderOptions = BorderFactory.createTitledBorder("Options");
        optionsPanel.setBorder(new CompoundBorder(paddingBorderOptions, titleBorderOptions));
        optionsPanel.add(regexCB);
        optionsPanel.add(matchCaseCB);
        optionsPanel.add(wholeWordCB);
        optionsPanel.add(wrapSearchCB);
        wrapSearchCB.setSelected(true);

        // Direction Panel
        JPanel directionPanel = new JPanel(new GridLayout(1, 2));
        Border paddingBorderDirection = new EmptyBorder(10, 10, 10, 10);
        Border titleBorderDirection = BorderFactory.createTitledBorder("Direction");
        directionPanel.setBorder(new CompoundBorder(paddingBorderDirection, titleBorderDirection));
        ButtonGroup group = new ButtonGroup();
        group.add(forwardSearch);
        group.add(backwardSearch);
        directionPanel.add(forwardSearch);
        directionPanel.add(backwardSearch);
        forwardSearch.setSelected(true);

        // Button Panel
        JPanel buttonPanel = new JPanel(new GridLayout(3, 3));

        // Add panels
        add(findReplacePanel);
        add(directionPanel);
        add(optionsPanel);
        add(buttonPanel);
        this.setBorder(new EmptyBorder(10, 10, 15, 15)); // Padding around panel

        // Search / Replace Field
        searchField = new JTextField(20);
        searchField.setText(textArea.getLastSearchedString());
        replaceField = new JTextField(20);
        replaceField.setText(textArea.getLastReplacedString());
        findReplacePanel.addItem("Find", searchField);
        findReplacePanel.addItem("Replace", replaceField);

        // Next Button
        final JButton nextButton = new JButton("Find");
        nextButton.addActionListener(e -> {
            textArea.setLastSearchedString(searchField.getText());
            SearchContext context = setUpContext();
            find(context);
        });
        buttonPanel.add(nextButton);
        frame.getRootPane().setDefaultButton(nextButton);

        // Replace / Find
        final JButton replaceFindButton = new JButton("Replace / Find");
        replaceFindButton.addActionListener(e -> {
            textArea.setLastSearchedString(searchField.getText());
            textArea.setLastReplacedString(replaceField.getText());
            SearchContext context = setUpContext();
            replace(context);
            find(context);
        });
        buttonPanel.add(replaceFindButton);

        // Replace Button
        JButton replaceButton = new JButton("Replace");
        replaceButton.addActionListener(e -> {
            textArea.setLastReplacedString(replaceField.getText());
            SearchContext context = setUpContext();
            replace(context);
        });
        buttonPanel.add(replaceButton);

        // Replace All Button
        final JButton replaceAllButton = new JButton("Replace All");
        replaceAllButton.addActionListener(e -> {
            textArea.setLastReplacedString(replaceField.getText());
            SearchContext context = setUpContext();
            if (context != null) {
                SearchResult replacements = SearchEngine.replaceAll(textArea, context);
                // TODO: Display number of replacements made in dialog
                // using replacements.getCount()
            }
        });
        buttonPanel.add(replaceAllButton);

        // Close Button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> frame.dispose());
        buttonPanel.add(closeButton);
    }

    /**
     * Set up the search context
     *
     * @return the prepared search context
     */
    private SearchContext setUpContext() {
        SearchContext context = new SearchContext();
        String text = searchField.getText();
        if (text.length() == 0) {
            return null;
        }
        context.setSearchFor(text);
        context.setReplaceWith(replaceField.getText());
        context.setSearchForward(forwardSearch.isSelected());
        context.setMatchCase(matchCaseCB.isSelected());
        context.setRegularExpression(regexCB.isSelected());
        context.setWholeWord(wholeWordCB.isSelected());

        return context;
    }

    /**
     * Find the next instance of the search string.
     *
     * @param context search context object
     */
    private void find(SearchContext context) {
        if (context != null) {
            SearchResult found = SearchEngine.find(textArea, context);
            if (!found.wasFound()) {
                if (wrapSearchCB.isSelected()) {
                    if (forwardSearch.isSelected()) {
                        textArea.setCaretPosition(0);
                    } else {
                        textArea.setCaretPosition(textArea.getText().length());
                    }
                }
            }
        }
    }

    /**
     * Replace the next instance of the replace string.
     *
     * @param context search context object
     */
    private void replace(SearchContext context) {
        if (context != null) {
            SearchEngine.replace((RTextArea) textArea, context);
        }

    }
}