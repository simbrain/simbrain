## General 

Simbrain apps are bundled with a JVM (the one used to build the distribution). Users should not need to install java on their machine to run Simbrain.

Note that all simulations are now stored inside the local application directory (the directory that contains the application itself). 

Certificates were obtained from UC Merced IT.

To activate console run jpackage with the -win-console command

Additional information about the builds are in comments to the build files referenced below.
## Mac build process
- `ant build` 
- `etc/make_app.sh`
- dmg produced in `dist`

<!-- UPDTAE BELOW BASED ON NEW SCRIPT -->
## PC build process 
- `ant build`
- `etc/make_app.bat`
- .exe installer produced in `dist`

## Linux build process
- Just unzip the file in `dist`, which has an executible jar 