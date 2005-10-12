export NAME="Simbrain2_win"
mkdir public_html
cp -rf ../dist ./$NAME
cp build.xml $NAME
cd $NAME
ant
rm build.xml
rm Simbrain.jar
cd ..
gnutar -czvf ./public_html/${NAME}.tar.gz $NAME 
rm -rf $NAME
