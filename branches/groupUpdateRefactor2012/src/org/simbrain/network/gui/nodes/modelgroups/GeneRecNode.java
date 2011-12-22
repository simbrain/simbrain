//package org.simbrain.network.gui.nodes.modelgroups;
//
//import java.awt.event.ActionEvent;
//
//import javax.swing.AbstractAction;
//import javax.swing.Action;
//
//import org.simbrain.network.groups.GeneRec;
//import org.simbrain.network.gui.NetworkPanel;
//import org.simbrain.network.gui.nodes.GroupNode;
//
//
///**
// * A PNode representing a GeneRec group.
// */
//public class GeneRecNode extends GroupNode {
//
//    /** Randomize action. */
//    private Action randomizeAction;
//
//    /**
//     * Constructor.
//     *
//     * @param networkPanel reference to parent panel.
//     * @param group the GeneRec group being represented.
//     */
//    public GeneRecNode(final NetworkPanel networkPanel, final GeneRec group) {
//        super(networkPanel, group);
//        randomizeAction = new AbstractAction("Randomize") {
//            public void actionPerformed(final ActionEvent event) {
//                group.randomize();
//            }
//        };
//        this.setConextMenu(getContextMenu());
//    }
//
////    /**
////     * Adds actions specific to GeneRec groups.
////     */
////    protected JPopupMenu getContextMenu() {
////
////        JPopupMenu ret = super.getContextMenu();
////        ret.addSeparator();
////        ret.add(randomizeAction);
////
////        return ret;
////    }
//
//}
