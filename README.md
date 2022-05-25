## Simbrain 

Main repository for Simbrain code, documentation, and simulations.

The default branch is currently Simbrain 4. To see code relating to Simbrain 3.0x select the `Simbrain3` branch.

## Install
Go to the [downloads](https://simbrain.net/Downloads/downloads_main.html) page and follow the directions.

To run from source see the instructions [here](https://github.com/simbrain/simbrain/wiki/Running-from-source).

## Getting Started

See this [getting started video](https://www.youtube.com/watch?v=yYzUmcPaurI)

Some things you can do to get a quick sense of how Simbrain works.

1) Open different workspaces using File > Open Workspace and press play in the in the workspace toolbar.

2) Run different scripts using the Script menu in the workspace menu, and pressing play in the workspace toolbar.

## License 
The GNU License is in the ./etc directory.

## Release Notes for Simbrain 3.05 
(Summer 2022)
- Simbrain is now packaged as a modern desktop application.
- Security issues with xstream using newer releases of java were addressed. Note that earlier versions of Simbrain will no longer run with the latest java (java 8 is recommended if running Simbrain 3.04) 
- Component open and save commands have been renamed to import / export, to avoid confusion between those functions and
  the preferred workspace open / save.
- Save dialog opens by default to local application directory.
- config.properties file removed
- Should be compatible with Simbrain 3.04

## Release Notes for Simbrain 3.04 
(Spring 2020)
- Added a series of behaviorism demos
- Drag is now ctrl-drag on PCs
- Some improvements to synapse selection

## Release Notes for Simbrain 3.03 
(Summer 2018)
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
