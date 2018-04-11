package org.simbrain.custom_sims.simulations.neat;

import java.util.List;
import static java.util.Objects.requireNonNull;

import org.simbrain.custom_sims.simulations.neat.procedureActions.InstanceProcedureAction;

/**
 * Procedure for agent use. E.g. a evaluation method.
 * @author LeoYulinLi
 *
 */
public class InstanceProcedure implements Procedure {

    /**
     * The target of the procedure.
     */
    private Agent agent;

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
//     * Construct an InstanceProcedure by specifying the agent with one single action as procedure.
//     * Mainly used when the procedure is custom made directly with Java.
//     * @param i The target of the procedure
//     * @param ipa A single action of the procedure.
//     */
//    public InstanceProcedure(Agent i, InstanceProcedureAction ipa) {
//        setAgent(i);
//        procedureAtcions = new ArrayList<>();
//        addProcedureAction(ipa);
//    }

    /**
     * Construct an InstanceProcedure by specifying the agent with a list of procedure actions.
     * @param i The target of the procedure
     * @param procedureAtcions A list of actions for procedure
     */
    public InstanceProcedure(Agent i, List<InstanceProcedureAction> procedureAtcions) {
        setAgent(i);
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
            pa.run(agent);
        }
    }

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = requireNonNull(agent);
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
