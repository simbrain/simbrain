## General 

Simbrain apps are bundled with a JVM (the one used to build the distribution). Users should not need to install java on their machine to run Simbrain.

Note that all simulations are now stored inside the local application directory (the directory that contains the application itself). 

Certificates were obtained from UC Merced IT.

To activate console run jpackage with the -win-console command

Additional information about the builds are in comments to the build files referenced below.

## Mac build process
- Use terminal to run `ant build`
- Got to `etc` and run `zsh make_app.sh` or press the play button at the top of the file
- dmg produced in `dist`

## PC build process 
- Same as mac but .exe installer produced in `dist`

## Linux build process
- Just unzip the file in `dist`, which has an executible jar 

## Debug
- If app fails to run go to `Simbrain.app/Contents/app` and invoke using `java -jar Simbrain.jar`