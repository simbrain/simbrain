export NAME="Simbrain2beta"
mkdir public_html
cp -rf ../dist ./$NAME
zip -r ./public_html/${NAME}.zip $NAME
rm -rf ./$NAME
rsync -avz -e ssh ./public_html/ simbrain@simbrain.net:public_html/Downloads
