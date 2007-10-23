package org.simbrain.gauge;

import org.simbrain.workspace.WorkspaceComponentListener;

public interface GaugeComponentListener extends WorkspaceComponentListener {
    void dimensionsChanged(int newDimensions);
}
