SIMBRAIN SCRIPTING

(If you are downloading from svn then some of these folders will be incomplete.   More sample scripts can be obtained by downloading Simbrain_2_AlphaX.zip).

This directory and its subdirectories hold beanshell scripts.  You are encouraged to add your own scripts.  This is your entry point for customizing Simbrain.

The syntax for these scripts is java (with some modifications).  In each case some base object is made available that you can modify using java commands. The best way to learn how to write these scripts is probably to just look (and modify)  existing scripts.  Also see http://www.beanshell.org/docs.html

Scripts in ./scriptmenu automatically appear in the "scripts" menu of the Simbrain workspace.   Use this directory to create actions that run custom simulations.  The two objects available here are "workspace" and "desktop", which give access to the current org.simbrain.workspace.Workspace object, and the gui representation via an org.simbrain.workspace.gui.SimbrainDesktop object.

Scripts in ./network  automatically appear in the "scripts" menu of Simbrain network components   Use this directory to create custom actions that can be applied to a current network (e.g., methods of adding neurons, modifying neurons or synapses, etc.)  The two objects available here are "network" and "networkPanel."  These give access to the current org.simbrain.network.interfaces.Rootnetwork and its gui representation via an org.simbrain.network.gui.NetworkPanel.

Scripts in ./console are available from the Simbrain console (from which command line arguments can be issued) or terminal components. The objects available here are the same as with scriptmenu.

Workspace is there as a place to put workspace that are called from scripts.  Workspaces from /simbrain/simulations/workspaces can be called, but those are more likely to be edited independently of the script, thereby breaking the script.