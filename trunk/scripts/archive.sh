export ARCHIVE_DIR="Documents/SimbrainARKIVES/zipped"
gnutar --exclude=*.gif --exclude=.svn -czvf temp.tar.gz ./src
mv temp.tar.gz src`date +%m%d%y`.tar.gz
mv *.gz $HOME/$ARCHIVE_DIR
