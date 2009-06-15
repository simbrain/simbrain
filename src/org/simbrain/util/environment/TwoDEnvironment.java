package org.simbrain.util.environment;

import java.util.List;

import org.simbrain.workspace.WorkspaceComponent;

/**
 * Simple 2d environment.
 * 
 * Assumed to exist in a workspace Component.
 */
public interface TwoDEnvironment {

	/** List of all entities in this environment. */
	public List<TwoDEntity> getTwoDEntityList();

	/** List of all smell sources. */
	public List<SmellSource> getSmellSources();

	/** Reference to WorkspaceComponet. */
	public WorkspaceComponent getParent();
	
}
