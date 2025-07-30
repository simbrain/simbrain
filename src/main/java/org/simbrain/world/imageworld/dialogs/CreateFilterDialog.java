package org.simbrain.world.imageworld.dialogs;

import org.simbrain.util.StandardDialog;
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor;
import org.simbrain.util.widgets.ShowHelpAction;
import org.simbrain.world.imageworld.filters.IdentityOp;
import org.simbrain.world.imageworld.filters.ImageTransformation;
import org.simbrain.world.imageworld.filters.ImageTransformationCollection;

import javax.swing.*;

/**
 * A dialog to create a new Filter.
 */
public class CreateFilterDialog extends StandardDialog {

    private final ImageTransformationCollection imageTransformationCollection;

    private final AnnotatedPropertyEditor editorPanel;

    private ImageTransformation templateImageTransformation;

    public CreateFilterDialog(ImageTransformationCollection imageTransformationCollection) {
        setTitle("Create Filter");
        this.imageTransformationCollection = imageTransformationCollection;

        // TODO: rename help
        ShowHelpAction helpAction = new ShowHelpAction("https://docs.simbrain.net/docs/worlds/imageworld.html");
        addButton(new JButton(helpAction));

        templateImageTransformation =
                new ImageTransformation("Filter " + (imageTransformationCollection.getFilters().size() + 1),
                        imageTransformationCollection.getImageSource(),
                        new IdentityOp(), 100, 100);

        editorPanel = new AnnotatedPropertyEditor(templateImageTransformation);
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
        templateImageTransformation.applyFilter();
        imageTransformationCollection.addFilter(templateImageTransformation);
        imageTransformationCollection.setCurrentFilter(templateImageTransformation);
    }
}
