package org.simbrain.util;

import org.simbrain.util.genericframe.GenericJDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * <b>StandardDialog</b> implements a standard data entry dialog with "Ok" and
 * "Cancel" buttons. Special functionality associated with Simbrain has also
 * been added. Subclasses can override the isDataValid(), okButtonPressed(), and
 * cancelButtonPressed() methods to perform implementation specific processing.
 * <p>
 * By default, the dialog is modal, and has a JPanel with a BorderLayout for its
 * content pane.
 *
 * @author David Fraser
 * @author Michael Harris
 * @author Jeff Yoshimi
 * @author Zoë Tosi
 */
public class StandardDialog extends GenericJDialog {

    /**
     * When this flag is set to true, then whenever a StandardDialog is used, a
     * warning will be displayed first, notifying the user that an external
     * simulation is running.
     */
    private static boolean isRunning;

    /**
     * Use this flag to disable the use of run warnings (see isRunning)
     */
    private static final boolean USE_RUN_WARNINGS = true;

    /**
     * Custom button panel.
     */
    private final JPanel customButtonPanel = new JPanel();

    /**
     * Action listener.
     */
    ActionListener actionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent actionEvent) {
            setVisible(false);
        }
    };

    /**
     * A global okay button for accessibility.
     */
    private JButton okButton;

    /**
     * A global cancel button for accessibility.
     */
    private JButton cancelButton;

    /**
     * Flag indicating if the "Cancel" button was pressed to close dialog.
     */
    private boolean myIsDialogCancelled = true;

    /**
     * The content pane for holding user components.
     */
    private Container myUserContentPane;

    /**
     * Tasks to execute whe closing with "Ok" (when cancel is pressed these
     * are _not_ invoked).
     */
    private final List<Runnable> invokeWhenCommitting = new ArrayList<>();

    private final List<Runnable> invokeWhenClosing = new ArrayList<>();

    /**
     * A lambda which, if false, prevents the dialog from being closed when the OK button is pressed. Can be used to
     * check the integrity of whatever is being set before it is committed.
     */
    private Supplier<Boolean> closingCheck = () -> true;

    /**
     * This method is the default constructor.
     */
    public StandardDialog() {
        super();
        init();
    }

    /**
     * Constructo a dialog around the provided panel.
     */
    public StandardDialog(JPanel panel) {
        super();
        init();
        setContentPane(panel);
    }

    /**
     * This method creates a StandardDialog with the given parent frame and
     * title.
     *
     * @param parent The parent frame for the dialog.
     * @param title  The title to display in the dialog.
     */
    public StandardDialog(Frame parent, String title) {
        super(parent, title);
        init();
    }
    
    /**
     * Create a StandardDialog as a child of the specified Window (supports Frame, Dialog, or any Window).
     */
    public StandardDialog(Window parent, String title) {
        super(parent, title);
        init();
    }

    /**
     * This method sets up the default attributes of the dialog and the content
     * pane.
     */
    private void init() {

        if (isRunning && USE_RUN_WARNINGS) {
            SwingUtilsKt.showWarningDialog("WARNING: You are modifying system parameters while a simulation is running. \n " + "It is reccomended that you first stop the simulation using the stop button.\n" + " Some functions may not behave as they are supposed to.");
        }

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Setup the internal content pane to hold the user content pane
        // and the standard button panel
        JPanel internalContentPane = new JPanel();

        internalContentPane.setLayout(new BorderLayout(0, Theme.sectionGap));

        internalContentPane.setBorder(Theme.dialogBorder());

        // Create the standard "Ok" Button
        Action okAction = new AbstractAction("OK") {
            public void actionPerformed(final ActionEvent actionEvent) {
                if (isValidData()) {
                    myIsDialogCancelled = false;
                    closeDialogOk();
                }
            }
        };

        // Create the standard "Cancel" Button
        Action cancelAction = new AbstractAction("Cancel") {
            public void actionPerformed(final ActionEvent actionEvent) {
                myIsDialogCancelled = true;
                closeDialogCancel();
                dispose();
            }
        };

        okButton = new JButton(okAction);
        okButton.setMnemonic('O');
        cancelButton = new JButton(cancelAction);
        cancelButton.setMnemonic('C');

        // Subclass-supplied buttons (e.g. Help) sit on the left; OK/Cancel are
        // right-aligned, the platform-conventional placement.
        customButtonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, Theme.componentGap, 0));

        JPanel okCancelPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.componentGap, 0));
        okCancelPanel.add(okButton);
        okCancelPanel.add(cancelButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(customButtonPanel, BorderLayout.WEST);
        buttonPanel.add(okCancelPanel, BorderLayout.EAST);

        getRootPane().setDefaultButton(okButton);

        internalContentPane.add(buttonPanel, BorderLayout.SOUTH);

        // Initialize the user content pane with a JPanel
        setContentPane(new JPanel(new BorderLayout()));

        super.setContentPane(internalContentPane);

        // Finally, add a listener for the window close button.
        // Process this event the same as the "Cancel" button.
        WindowAdapter windowAdapter = new WindowAdapter() {
            public void windowClosing(final WindowEvent windowEvent) {
                myIsDialogCancelled = true;
                closeDialogCancel(); // closeDialogCancel() now handles running close tasks
            }
        };

        addWindowListener(windowAdapter);

        this.getRootPane().registerKeyboardAction(actionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK), JComponent.WHEN_IN_FOCUSED_WINDOW);
        this.getRootPane().registerKeyboardAction(actionListener,
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // this.setAlwaysOnTop(true); //BREAKS SOME VERSIONS
        pack();

    }

    /**
     * Adds a function to invoke when committing the dialog (OK/Done button only).
     * Note: Must call this before making the dialog visible!
     *
     * @param task a Runnable to execute when committing
     */
    public void addCommitTask(Runnable task) {
        invokeWhenCommitting.add(task);
    }

    /**
     * Adds a function to invoke when closing the dialog (ALL close methods: OK, Cancel, X button).
     * Note: Must call this before making the dialog visible!
     *
     * @param task a Runnable to execute when closing
     */
    public void addCloseTask(Runnable task) {
        invokeWhenClosing.add(task);
    }

    public void setClosingCheck(Supplier<Boolean> closingCheck) {
        this.closingCheck = closingCheck;
    }

    /**
     * Override to perform specific clean up when dialog closed.
     */
    protected void closeDialogOk() {
        if (!closingCheck.get()) {
            return;
        }
        for (Runnable task : invokeWhenCommitting) {
            task.run();
        }
        // Run close tasks for OK button as well - close tasks should run for ALL dialog closures
        for (Runnable task : invokeWhenClosing) {
            task.run();
        }
        dispose();
    }

    /**
     * Override to perform specific clean up when dialog closed.
     */
    protected void closeDialogCancel() {
        // Run close tasks for Cancel button
        for (Runnable task : invokeWhenClosing) {
            task.run();
        }
        dispose();
    }

    /**
     * This method gets the content pane for adding components. Components
     * should not be added directly to the dialog.
     *
     * @return the content pane for the dialog.
     */
    public Container getContentPane() {
        return myUserContentPane;
    }

    /**
     * This method sets the content pane for adding components. Components
     * should not be added directly to the dialog.
     *
     * @param contentPane The content pane for the dialog.
     */
    public void setContentPane(final Container contentPane) {
        myUserContentPane = contentPane;

        super.getContentPane().add(myUserContentPane, BorderLayout.CENTER);
    }

    /**
     * This method returns <code>true</code> if the User cancelled the dialog
     * otherwise <code>false</code>. The dialog is cancelled if the "Cancel"
     * button is pressed or the "Close" window button is pressed, or the
     * "Escape" key is pressed. In other words, if the User has caused the
     * dialog to close by any method other than by pressing the "Ok" button,
     * this method will return <code>true</code>.
     *
     * @return
     */
    public boolean hasUserCancelled() {
        return myIsDialogCancelled;
    }

    /**
     * This method is used to validate the current dialog box. This method
     * provides a default response of <code>true</code>. This method should be
     * implemented by each dialog that extends this class.
     *
     * @return a boolean indicating if the data is valid. <code>true</code>
     * indicates that all of the fields were validated correctly and
     * <code>false</code> indicates the validation failed
     */
    protected boolean isValidData() {
        return true;
    }

    /**
     * Adds a new button to the panel.
     *
     * @param theButton button to be added
     */
    public void addButton(final JButton theButton) {
        customButtonPanel.add(theButton);
    }

    /**
     * @param isRunning the isRunning to set
     */
    public static void setSimulationRunning(boolean isRunning) {
        StandardDialog.isRunning = isRunning;
    }

    /**
     * Center the dialog on the screen.
     */
    public void centerDialog() {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final Dimension screenSize = toolkit.getScreenSize();
        final int x = (screenSize.width - getWidth()) / 2;
        final int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
    }

    /**
     * Disables the Ok button if certain constraints of the dialog have not be
     * fulfilled.
     */
    public void disableOkButton() {
        okButton.setEnabled(false);
    }

    /**
     * Enables the Ok button.
     */
    public void enableOkButton() {
        okButton.setEnabled(true);
    }

    /**
     * Give other classes access to the Ok button.
     *
     * @return the ok JButton
     */
    public JButton getOkButton() {
        return okButton;
    }

    public JButton getCancelButton() {
        return cancelButton;
    }

    /**
     * If called then this becomes a "Done" dialog. The cancel button is removed
     * and "Ok" is renamed "Done".
     */
    public void setAsDoneDialog() {
        okButton.setText("Done");
        cancelButton.setVisible(false);
    }

    /**
     * Standard operation to make the dialog visible and centered.
     */
    public void makeVisible() {
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

}
