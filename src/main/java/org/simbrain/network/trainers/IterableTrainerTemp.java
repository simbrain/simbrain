package org.simbrain.network.trainers;

import org.simbrain.network.events.TrainerEvents;
import org.simbrain.util.propertyeditor.EditableObject;

// TODO: Rename after refactor all trainerX classes...
public interface IterableTrainerTemp extends EditableObject {

    int getIteration();

    double getError();

    boolean isUpdateCompleted();

    void setUpdateCompleted(boolean b);

    void iterate() throws Trainer.DataNotInitializedException;

    void commitChanges();

    void randomize();

    void revalidateSynapseGroups();

    TrainerEvents getEvents();
}
