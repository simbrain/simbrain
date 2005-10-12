export NAME="Simbrain2_mac"
mkdir public_html
cp -rf ../dist ./$NAME
cp -rf mac/Simbrain2.app/ $NAME/Simbrain2.app
gnutar -czvf ./public_html/${NAME}.tar.gz $NAME 
rm -rf ./$NAME
