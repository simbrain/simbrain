export NAME="Simbrain2_linux"
mkdir public_html
cp -rf ../dist ./$NAME
gnutar -czvf ./public_html/${NAME}.tar.gz $NAME 
rm -rf ./$NAME
