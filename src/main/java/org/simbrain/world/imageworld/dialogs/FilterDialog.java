package org.simbrain.world.imageworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.ImageSourceProperties;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterCollection;

import javax.swing.*;

/**
 * A dialog to create a new Filter.
 */
public class FilterDialog extends StandardDialog {

    private final FilterCollection filterCollection;

    private final AnnotatedPropertyEditor editorPanel;

    private final ImageSourceProperties imageSourceProperties = new ImageSourceProperties();

    public FilterDialog(FilterCollection filterCollection) {
        setTitle("Create Filter");
        this.filterCollection = filterCollection;
        // TODO: rename help
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/sensorMatrix.html");
        addButton(new JButton(helpAction));

        imageSourceProperties.setName("Filter " + (filterCollection.getFilters().size() + 1));

        editorPanel = new AnnotatedPropertyEditor(imageSourceProperties);
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
        String name = imageSourceProperties.getName();
        Filter newFilter = new Filter(
                name,
                filterCollection.getImageSource(),
                imageSourceProperties.getColorOp(),
                imageSourceProperties.getWidth(),
                imageSourceProperties.getHeight()
        );
        filterCollection.addFilter(newFilter);
    }
}
