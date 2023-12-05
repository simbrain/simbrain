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

## Build process

All builds are coordinated using `build.gradle.kts`

- For mac, use the `signMacApp` task then `pushMacInstaller`
- For pc, use the `signWindowsApp` task then `pushWindowsInstaller`
- For Linux, use `createZip` then `pushZip`

Note: notarization (only used on Mac) is not yet implemented

Debugging the install. If app fails to run go to `Simbrain.app/Contents/app` and invoke using `java -jar Simbrain.jar`