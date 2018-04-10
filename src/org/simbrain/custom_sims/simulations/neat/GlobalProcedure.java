package org.simbrain.custom_sims.simulations.neat;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.simulations.neat.procedureActions.GlobalProcedureAction;

/**
 * Procedure for global use (does not require a Instance argument).
 * @author LeoYulinLi
 *
 */
public class GlobalProcedure implements Procedure{
    private static int currentId = 0;
    protected List<GlobalProcedureAction> procedureAtcions;
    private int id;
    private String name;

    public void run() {
        for (GlobalProcedureAction pa : procedureAtcions) {
            pa.run();
        }
    }

    public GlobalProcedure() {
        procedureAtcions = new ArrayList<>();
        id = currentId;
        currentId++;
        name = "";
    }

    public void addProcedureAction(GlobalProcedureAction pa) {
        procedureAtcions.add(pa);
    }
    
    public List<GlobalProcedureAction> getProcedures() {
        return procedureAtcions;
    }

    public void setProcedures(List<GlobalProcedureAction> procedureActions) {
        this.procedureAtcions = procedureActions;
    }
    

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
