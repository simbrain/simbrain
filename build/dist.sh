export NAME="Simbrain2beta"
mkdir public_html
cp -rf ../dist ./$NAME
gnutar -czvf ./public_html/${NAME}.tar.gz $NAME 
rm -rf ./$NAME
rsync -avz -e ssh ./public_html/ simbrain@simbrain.net:public_html/Downloads
