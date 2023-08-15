## Mac build process
- Use terminal from `simbrain` home dir to run `ant build`
- Got to `etc` and run `zsh make_app.sh` or press the play button at the top of the file
- Run `package_and_notarize` in the same way

## PC build process 
- Follow directions in `make_app.ps1`

## Linux build process
- Linux just uses the zip in `dist` so the push downloads script takes care of that after the build is run

## Push downloads to website 
- `zsh put_downloads.sh`

## Debug
- If app fails to run go to `Simbrain.app/Contents/app` and invoke using `java -jar Simbrain.jar`