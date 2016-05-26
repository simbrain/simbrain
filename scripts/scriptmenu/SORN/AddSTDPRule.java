import org.simbrain.network.core.*;
import org.simbrain.network.synapse_update_rules.*;

public class AddSTDPRule extends STDPRule {

    private boolean srcSpk = false;

    private boolean tarSpk = false;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "STDP";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        STDPRule duplicateSynapse = new STDPRule();
        duplicateSynapse.setTau_minus(this.getTau_minus());
        duplicateSynapse.setTau_plus(this.getTau_plus());
        duplicateSynapse.setW_minus(this.getW_minus());
        duplicateSynapse.setW_plus(this.getW_plus());
        duplicateSynapse.setLearningRate(this.getLearningRate());
        return duplicateSynapse;
    }

    @Override
    public void update(Synapse synapse) {
        boolean ss = synapse.getSource().isSpike();
        boolean st = synapse.getTarget().isSpike();

        double str = synapse.getStrength();

        str += learningRate * ((srcSpk && st ? 1 : 0) - (tarSpk && ss ? 1 : 0));
        synapse.setStrength(synapse.clip(str));
        srcSpk = ss;
        tarSpk = st;

    }
}
