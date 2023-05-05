## Mac build process
- Use terminal from `simbrain` dir to run `ant build`
- Got to `etc` and run `zsh make_app.sh` or press the play button at the top of the file

## PC build process 
- Same as mac but .exe installer produced in `dist`

## Linux build process
- Linux just uses the zip in `dist` so the push downloads script takes care of that after the build is run

## Package and sign
- `zsh package_and_sign.sh`

## Push downloads to website
- `zsh put_downloads.sh`

## Debug
- If app fails to run go to `Simbrain.app/Contents/app` and invoke using `java -jar Simbrain.jar`