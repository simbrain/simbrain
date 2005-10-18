export NAME="Simbrain2_win"
mkdir public_html
cp -rf ../dist ./$NAME
cp build.xml $NAME
cd $NAME
ant
rm build.xml
rm Simbrain.jar
cd ..
zip -r ./public_html/${NAME}.zip $NAME 
rm -rf $NAME
