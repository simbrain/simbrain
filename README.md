# Simbrain 

Main repository for Simbrain code, documentation, and simulations.

The default branch is currently Simbrain 4. To see code relating to Simbrain 3.0x select `master` branch.

The GNU License is in the ./etc directory.

## Release Notes for Simbrain 3.05 
(Fall 2021, pending)
- Simbrain is no longer launched via the jar, but is packaged as a modern desktop application.
- Some security issues with xstream have been addressed
- Component open and save commands have been renamed to import / export, to avoid confusion between those functions and
  the preferred workspace open / save.
- Save dialog opens by default to system-specific "my documents" folder
- config.properties file removed

## Release Notes for Simbrain 3.04 
(Spring 2020)
- Added a series of behaviorism demos
- Drag is now ctrl-drag on PCs
- Some improvements to synapse selection

## Release Notes for Simbrain 3.03 
(Summer 2018)
- This is a last or nearly last release in the Simbrain 3.0 series. Development efforts are shifting to 3.1
- It may be possible to open some 3.02 workspace or networks in 3.03, but results will vary.
- Improved clamping. Select nodes and / or synapses and click Shift-F. Repeat to toggle. A bold outline indicates clamped nodes or frozen synapses.
- Improved zooming / panning. Now based on Command / Control drag (rather than Shift-drag). This makes it possible to select items while zoomed in. Also auto-zoom mode changes are now based on single clicks, which is more intuitive.
- Added a command for resizing all windows. Renamed old window-gathering method "gather windows"
- Improved lasso selection of self-connections and weights generally
- Improved coloring for wand on PC
- Key command to clear workspace: Command/Control-k.
- Key command to duplicate items: Command/ Control-d.
- Key command to create neuron groups: g
- Key command to "convert" loose neurons to neuron group: Shift-G
- Key command to unselect: Escape
- Eraser button repurposed to set nodes only (selected or not) to 0; associated with key command 'k'
- Extending internationalization fix to synapse groups
- New simulation framework, to supplement scripting. Custom simulations are now made using an IDE. See org.simbrain.simulation in the source. Simulations will have to eventually be ported to 3.1 (where development efforts are shifting), but since simulations are developed in an IDE the porting should be much easier.
- Misc. bug fixes (e.g. control-w no longer closes windows without asking whether they should be saved; odor worlds no longer retain coupling values when closed, etc.).
- Misc. documentation improvements, e.g. in integrate and fire.

## Release Notes for Simbrain 3.02 
(Winter 2016)
- This release is discussed in Tosi and Yoshimi 2016, DOI: 10.1016/j.neunet.2016.07.005
- Some changes to the user interface. Removing toggle auto-zoom command. Repositioning and toggling auto-zoom are now done by single or double-clicking the same button.
- Localized Neuron Text Fields to address issue #35
- Getting rid of extranneous labels in plots
- Adding Alex Holcomb's latest sims and some minor updates to some workspace docs
- Do not fail if properties file is not present.
- Improvements to SORN Script and other minor script improvements
- Zoom wheel now zooms about the mouse rather than the center of the screen.
- Panning now based on shift rather than "meta"
- Improved tooltips for neurons
