export ARCHIVE_DIR="Documents/SimbrainARKIVES/zipped"
tar -cvf temp.tar ./src 
gzip temp.tar
mv temp.tar.gz src2`date +%m%d%y`.tar.gz
mv *.gz $HOME/$ARCHIVE_DIR
