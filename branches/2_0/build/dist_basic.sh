export NAME="Simbrain_2"
mkdir public_html
cp -rf ../dist ./$NAME
zip -r ./public_html/${NAME}.zip $NAME
rm -rf ./$NAME
