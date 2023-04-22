# 1. Ant build
# 2. Run the command at the bottom in power shell
# 3. (skip if have done previously) install the .p12 certificate by double clicking the .p12 file and entering the password
# 4. sign with signtool sign /a /fd SHA256 ..\dist\Simbrain-3.05.exe
jpackage --input ..\build\main\ `
    --main-jar Simbrain.jar `
    --dest ..\dist `
    --name "Simbrain" `
    --app-version 3.06 `
    --icon simbrain.ico `
    --java-options "-Duser.dir=`$APPDIR --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.desktop/java.awt=ALL-UNNAMED --add-opens=java.base/java.util.concurrent=ALL-UNNAMED" `
    --win-menu --win-menu-group Simbrain --vendor Simbrain --verbose
