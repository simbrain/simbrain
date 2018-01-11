package org.simbrain.world.threedworld.entities;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public class WalkingEffector implements Effector {
    private class WalkingEffectorEditor extends EffectorEditor {
        private JFormattedTextField walkSpeedField = new JFormattedTextField(EditorDialog.floatFormat);
        private JFormattedTextField turnSpeedField = new JFormattedTextField(EditorDialog.floatFormat);
        private JComboBox<String> idleAnimNameField = new JComboBox<String>();
        private JComboBox<String> walkAnimNameField = new JComboBox<String>();
        private JComboBox<String> turnAnimNameField = new JComboBox<String>();
        private JFormattedTextField idleAnimSpeedField = new JFormattedTextField(EditorDialog.floatFormat);
        private JFormattedTextField walkAnimSpeedField = new JFormattedTextField(EditorDialog.floatFormat);
        private JFormattedTextField turnAnimSpeedField = new JFormattedTextField(EditorDialog.floatFormat);

        {
            walkSpeedField.setColumns(5);
            turnSpeedField.setColumns(5);
            idleAnimSpeedField.setColumns(5);
            walkAnimSpeedField.setColumns(5);
            turnAnimSpeedField.setColumns(5);
        }

        private WalkingEffectorEditor() {
            super(agent, WalkingEffector.this);
            idleAnimNameField.addItem(NoAnimation);
            walkAnimNameField.addItem(NoAnimation);
            turnAnimNameField.addItem(NoAnimation);
            for (String name : agent.getModel().getAnimations()) {
                idleAnimNameField.addItem(name);
                walkAnimNameField.addItem(name);
                turnAnimNameField.addItem(name);
            }
        }

        @Override
        public JComponent layoutFields() {
            JComponent effectorComponent = super.layoutFields();

            getPanel().add(new JLabel("Speed"), "skip");
            getPanel().add(new JLabel("Animation"));
            getPanel().add(new JLabel("Anim. Speed"), "wrap");

            getPanel().add(new JLabel("Idle"));
            getPanel().add(idleAnimNameField, "skip");
            getPanel().add(idleAnimSpeedField, "wrap");

            getPanel().add(new JLabel("Walk"));
            getPanel().add(walkSpeedField);
            getPanel().add(walkAnimNameField);
            getPanel().add(walkAnimSpeedField, "wrap");

            getPanel().add(new JLabel("Turn"));
            getPanel().add(turnSpeedField);
            getPanel().add(turnAnimNameField);
            getPanel().add(turnAnimSpeedField, "wrap");

            return effectorComponent;
        }

        @Override
        public void readValues() {
            walkSpeedField.setValue(getWalkSpeed());
            turnSpeedField.setValue(getTurnSpeed());
            idleAnimNameField.setSelectedItem(getIdleAnimName());
            walkAnimNameField.setSelectedItem(getWalkAnimName());
            turnAnimNameField.setSelectedItem(getTurnAnimName());
            idleAnimSpeedField.setValue(getIdleAnimSpeed());
            walkAnimSpeedField.setValue(getWalkAnimSpeed());
            turnAnimSpeedField.setValue(getTurnAnimSpeed());
        }

        @Override
        public void writeValues() {
            setWalkSpeed(((Number) walkSpeedField.getValue()).floatValue());
            setTurnSpeed(((Number) turnSpeedField.getValue()).floatValue());
            setIdleAnimName((String) idleAnimNameField.getSelectedItem());
            setWalkAnimName((String) walkAnimNameField.getSelectedItem());
            setTurnAnimName((String) turnAnimNameField.getSelectedItem());
            setIdleAnimSpeed(((Number) idleAnimSpeedField.getValue()).floatValue());
            setWalkAnimSpeed(((Number) walkAnimSpeedField.getValue()).floatValue());
            setTurnAnimSpeed(((Number) turnAnimSpeedField.getValue()).floatValue());
        }
    }

    public static String NoAnimation = "None";

    private Agent agent;
    private float walkSpeed = 3;
    private float turnSpeed = 3;
    private String idleAnimName = "Idle";
    private String walkAnimName = "Walk";
    private String turnAnimName = "Idle";
    private float idleAnimSpeed = 1;
    private float walkAnimSpeed = 1;
    private float turnAnimSpeed = 1;
    private float walking = 0;
    private float turning = 0;

    public WalkingEffector(Agent agent) {
        this.agent = agent;
        agent.addEffector(this);
        agent.getModel().setKinematic(true);
        agent.getModel().setAnimation(idleAnimName, idleAnimSpeed);
    }

    /**
     * @return Return a deserialized WalkingEffector.
     */
    public Object readResolve() {
        agent.getModel().setKinematic(true);
        agent.getModel().setAnimation(idleAnimName, idleAnimSpeed);
        return this;
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public void setWalkSpeed(float value) {
        walkSpeed = value;
    }

    public float getTurnSpeed() {
        return turnSpeed;
    }

    public void setTurnSpeed(float value) {
        turnSpeed = value;
    }

    public String getIdleAnimName() {
        return idleAnimName;
    }

    public void setIdleAnimName(String value) {
        idleAnimName = value;
    }

    public String getWalkAnimName() {
        return walkAnimName;
    }

    public String getTurnAnimName() {
        return turnAnimName;
    }

    public void setTurnAnimName(String value) {
        turnAnimName = value;
    }

    public void setWalkAnimName(String value) {
        walkAnimName = value;
    }

    public float getIdleAnimSpeed() {
        return idleAnimSpeed;
    }

    public void setIdleAnimSpeed(float value) {
        idleAnimSpeed = value;
    }

    public float getWalkAnimSpeed() {
        return walkAnimSpeed;
    }

    public void setWalkAnimSpeed(float value) {
        walkAnimSpeed = value;
    }

    public float getTurnAnimSpeed() {
        return turnAnimSpeed;
    }

    public void setTurnAnimSpeed(float value) {
        turnAnimSpeed = value;
    }

    @Producible(idMethod="getId")
    public double getWalking() {
        return walking;
    }

    public boolean isWalking() {
        return FastMath.abs(walking) > 0.01;
    }

    public void setWalking(double value) {
        walking = (float) value;
    }

    @Consumable(description="setWalking", idMethod="getId")
    public void queueWalking(double value) {
        agent.getEngine().enqueue(() -> {
            setWalking((float) value);
        });
    }

    @Producible(idMethod="getId")
    public float getTurning() {
        return turning;
    }

    public boolean isTurning() {
        return FastMath.abs(turning) > 0.01;
    }

    public void setTurning(float value) {
        turning = value;
    }

    @Consumable(description="setTurning", idMethod="getId")
    public void queueTurning(double value) {
        agent.getEngine().enqueue(() -> {
            setTurning((float) value);
        });
    }

    @Override
    public void update(float tpf) {
        updateAnimation();
        applyMovement(tpf);
    }

    private void updateAnimation() {
        ModelEntity model = agent.getModel();
        String animation = model.getAnimation();
        if (isWalking() && !walkAnimName.equals(NoAnimation)) {
            if (!walkAnimName.equals(animation)) {
                model.setAnimation(walkAnimName, walkAnimSpeed);
            }
        } else if (isTurning() && !turnAnimName.equals(NoAnimation)) {
            if (!turnAnimName.equals(animation)) {
                model.setAnimation(turnAnimName, turnAnimSpeed);
            }
        } else if (!idleAnimName.equals(NoAnimation)) {
            if (!idleAnimName.equals(animation)) {
                model.setAnimation(idleAnimName, idleAnimSpeed);
            }
        }
    }

    private void applyMovement(float tpf) {
        Quaternion rotation = new Quaternion();
        rotation.fromAngles(0, getTurning() * getTurnSpeed() * tpf, 0);
        agent.rotate(rotation);
        Vector3f direction = agent.getRotation().mult(Vector3f.UNIT_Z);
        Vector3f offset = direction.mult((float) getWalking() * getWalkSpeed() * tpf);
        agent.move(offset);
    }

    @Override
    public void delete() {
        agent.getModel().setKinematic(false);
        agent.removeEffector(this);
    }

    @Override
    public EffectorEditor getEditor() {
        return new WalkingEffectorEditor();
    }
}