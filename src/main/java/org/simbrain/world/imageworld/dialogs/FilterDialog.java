package org.simbrain.world.imageworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.ImageSourceProperties;
import org.simbrain.world.imageworld.filters.Filter;
import org.simbrain.world.imageworld.filters.FilterSelector;
import org.simbrain.world.imageworld.filters.FilteredImageSource;

import javax.swing.*;

/**
 * A dialog to create a new Filter.
 */
public class FilterDialog extends StandardDialog {

    private FilterSelector filterSelector;
    private AnnotatedPropertyEditor editorPanel;
    private ImageSourceProperties imageSourceMeta = new ImageSourceProperties();

    public FilterDialog(FilterSelector filterSelector) {
        setTitle("Create Filter");
        this.filterSelector = filterSelector;
        // TODO: rename help
        ShowHelpAction helpAction = new ShowHelpAction("Pages/Worlds/ImageWorld/sensorMatrix.html");
        addButton(new JButton(helpAction));

        imageSourceMeta.setName("Filter " + (filterSelector.getFilters().size() + 1));

        editorPanel = new AnnotatedPropertyEditor(imageSourceMeta);
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
        String name = imageSourceMeta.getName();
        // FilteredImageSource filter = filterFactory.create(world.getCompositeImageSource());
        FilteredImageSource filter = new FilteredImageSource(
                filterSelector.getImageSource(),
                imageSourceMeta.getColorOp(),
                imageSourceMeta.getWidth(),
                imageSourceMeta.getHeight()
        );
        Filter newFilter = new Filter(name, filter);
        filterSelector.addFilterContainer(newFilter);
    }
}
