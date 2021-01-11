package org.simbrain.world.imageworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterCollection;
import org.simbrain.world.imageworld.filters.IdentityOp;

import javax.swing.*;

/**
 * A dialog to create a new Filter.
 */
public class CreateFilterDialog extends StandardDialog {

    private final FilterCollection filterCollection;

    private final AnnotatedPropertyEditor editorPanel;

    private Filter templateFilter;

    public CreateFilterDialog(FilterCollection filterCollection) {
        setTitle("Create Filter");
        this.filterCollection = filterCollection;

        // TODO: rename help
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/sensorMatrix.html");
        addButton(new JButton(helpAction));

        templateFilter =
                new Filter("Filter " + (filterCollection.getFilters().size() + 1),
                        filterCollection.getImageSource(),
                        new IdentityOp(), 100, 100);

        editorPanel = new AnnotatedPropertyEditor(templateFilter);
        Box mainPanel = Box.createVerticalBox();
        mainPanel.add(editorPanel);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    protected void closeDialogOk() {
        super.closeDialogOk();
        commitChanges();
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        editorPanel.commitChanges();
        filterCollection.addFilter(templateFilter);
    }
}
