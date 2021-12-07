@REM Ant build and run this
jpackage --input ..\build\main\ --main-jar Simbrain.jar --dest ..\dist --name "Simbrain" --app-version 3.05 --icon simbrain.ico --java-options "-Duser.dir=`$APPDIR --illegal-access=permit" --win-menu --win-menu-group Simbrain --vendor Simbrain --verbose
@REM install the .p12 certificate by double clicking and entering the password
@REM and sign with signtool sign /a /fd SHA256 ..\dist\Simbrain-3.05.exe