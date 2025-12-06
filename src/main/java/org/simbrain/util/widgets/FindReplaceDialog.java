package org.simbrain.util.widgets;

import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;

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
    private JCheckBox regexCB = new JCheckBox("Regex");

    /**
     * Match case.
     */
    private JCheckBox matchCaseCB = new JCheckBox("Match Case");

    /**
     * Whole word.
     */
    private JCheckBox wholeWordCB = new JCheckBox("Whole Word");

    /**
     * Wrap search.
     */
    private JCheckBox wrapSearchCB = new JCheckBox("Wrap");

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

    /**
     * Whether to show replace functionality.
     */
    private final boolean showReplace;

    public FindReplaceDialog(final JFrame frame, final SimbrainTextArea textArea) {
        this.textArea = textArea;
        this.showReplace = textArea.isEditable();

        // Use MigLayout for cleaner layout
        // Layout: label | text field | buttons
        setLayout(new MigLayout(
            "insets 15, gap 8",  // Layout constraints
            "[right][grow, fill][fill][fill]",  // Column constraints
            "[]8[]12[]8[]"  // Row constraints
        ));

        // Initialize fields
        searchField = new JTextField(16);
        searchField.setText(textArea.getLastSearchedString());

        // Create buttons
        JButton findButton = new JButton("Find Next");
        findButton.addActionListener(e -> {
            textArea.setLastSearchedString(searchField.getText());
            SearchContext context = setUpContext();
            find(context);
        });

        // Row 1: Find field and Find button
        add(new JLabel("Find:"));
        add(searchField, "growx");

        if (showReplace) {
            add(findButton);
            JButton replaceFindButton = new JButton("Replace & Find");
            replaceFindButton.addActionListener(e -> {
                textArea.setLastSearchedString(searchField.getText());
                textArea.setLastReplacedString(replaceField.getText());
                SearchContext context = setUpContext();
                replace(context);
                find(context);
            });
            add(replaceFindButton, "wrap");

            // Row 2: Replace field and Replace buttons
            replaceField = new JTextField(16);
            replaceField.setText(textArea.getLastReplacedString());

            JButton replaceButton = new JButton("Replace");
            replaceButton.addActionListener(e -> {
                textArea.setLastReplacedString(replaceField.getText());
                SearchContext context = setUpContext();
                replace(context);
            });

            JButton replaceAllButton = new JButton("Replace All");
            replaceAllButton.addActionListener(e -> {
                textArea.setLastReplacedString(replaceField.getText());
                SearchContext context = setUpContext();
                if (context != null) {
                    SearchResult replacements = SearchEngine.replaceAll(textArea, context);
                    JOptionPane.showMessageDialog(frame,
                        replacements.getCount() + " occurrence(s) replaced.",
                        "Replace All",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });

            add(new JLabel("Replace:"));
            add(replaceField, "growx");
            add(replaceButton);
            add(replaceAllButton, "wrap");
        } else {
            // Find-only mode: span the button across both button columns
            add(findButton, "span 2, wrap");
        }

        // Row 3: Options (checkboxes) - skip first column to align with text fields
        add(new JLabel());  // Empty label column
        JPanel optionsPanel = new JPanel(new MigLayout("insets 0, gap 15", "[][][][]", "[]"));
        optionsPanel.add(matchCaseCB);
        optionsPanel.add(wholeWordCB);
        optionsPanel.add(regexCB);
        optionsPanel.add(wrapSearchCB);
        wrapSearchCB.setSelected(true);

        add(optionsPanel, "span 3, align left, wrap");

        // Row 4: Direction and Close button - skip first column to align with text fields
        add(new JLabel());  // Empty label column
        JPanel directionPanel = new JPanel(new MigLayout("insets 0, gap 10", "[][]", "[]"));
        ButtonGroup group = new ButtonGroup();
        group.add(forwardSearch);
        group.add(backwardSearch);
        forwardSearch.setSelected(true);
        directionPanel.add(forwardSearch);
        directionPanel.add(backwardSearch);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> frame.dispose());

        add(directionPanel);
        add(new JLabel(), "growx");  // Spacer
        add(closeButton, "align right, wrap");

        // Set default button
        frame.getRootPane().setDefaultButton(findButton);
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
        if (replaceField != null) {
            context.setReplaceWith(replaceField.getText());
        }
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
