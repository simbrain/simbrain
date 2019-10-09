package org.simbrain.network.trainers;

import org.simbrain.util.propertyeditor.EditableObject;

// TODO: Rename after refactor all trainerX classes...
public interface IterableTrainerTemp extends EditableObject {

    
    void removeErrorListener(ErrorListener errorListener);

    int getIteration();

    void addErrorListener(ErrorListener e);

    double getError();

    boolean isUpdateCompleted();

    void setUpdateCompleted(boolean b);

    void iterate() throws Trainer.DataNotInitializedException;

    void commitChanges();

    void randomize();

}
