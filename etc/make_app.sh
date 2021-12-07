# Assumes "ant build" has been called
jpackage --input ../build/main/	 --main-jar Simbrain.jar --dest ../dist --name "Simbrain" \
	 --app-version 3.05 --icon simbrain.icns --java-options "-Duser.dir=\$APPDIR --illegal-access=permit"

# To code sign, unpackage the dmg after it's made, go to the folder it is in (dist) and run
# `codesign -fs "University of California, Merced" Simbrain.app`
# Then repackage it into a dmg by placing it in a folder and using disk utility File > New Image

# TODO: fold code-signing into the jpackage command