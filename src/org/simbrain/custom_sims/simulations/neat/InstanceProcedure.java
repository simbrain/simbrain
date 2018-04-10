package org.simbrain.custom_sims.simulations.neat;

import java.util.List;
import static java.util.Objects.requireNonNull;

import org.simbrain.custom_sims.simulations.neat.procedureActions.InstanceProcedureAction;

/**
 * Procedure for instance use. E.g. a evaluation method.
 * @author LeoYulinLi
 *
 */
public class InstanceProcedure implements Procedure {

    /**
     * The target of the procedure.
     */
    private Instance instance;

    /**
     * The actions of the procedure.
     */
    private List<InstanceProcedureAction> procedureAtcions;

    /**
     * Not used for now. Will be use in a ProcedureManager when implemented.
     */
    private int id;

    /**
     * Not used for now. Will be use in a ProcedureManager when implemented.
     */
    private String name;

//    /**
//     * Construct an InstanceProcedure by specifying the instance with one single action as procedure.
//     * Mainly used when the procedure is custom made directly with Java.
//     * @param i The target of the procedure
//     * @param ipa A single action of the procedure.
//     */
//    public InstanceProcedure(Instance i, InstanceProcedureAction ipa) {
//        setInstance(i);
//        procedureAtcions = new ArrayList<>();
//        addProcedureAction(ipa);
//    }

    /**
     * Construct an InstanceProcedure by specifying the instance with a list of procedure actions.
     * @param i The target of the procedure
     * @param procedureAtcions A list of actions for procedure
     */
    public InstanceProcedure(Instance i, List<InstanceProcedureAction> procedureAtcions) {
        setInstance(i);
        setProcedureAction(procedureAtcions);
    }

//    /**
//     * Add a procedure action to this procedure.
//     * @param ipa A single action to add
//     */
//    public void addProcedureAction(InstanceProcedureAction ipa) {
//        procedureAtcions.add(requireNonNull(ipa));
//    }

    public void setProcedureAction(List<InstanceProcedureAction> procedureAtcions) {
        this.procedureAtcions = procedureAtcions;
    }

    public List<InstanceProcedureAction> getProcedureAtcions() {
        return this.procedureAtcions;
    }

    @Override
    public void run() {
        for (InstanceProcedureAction pa : procedureAtcions) {
            pa.run(instance);
        }
    }

    public Instance getInstance() {
        return instance;
    }

    public void setInstance(Instance instance) {
        this.instance = requireNonNull(instance);
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
