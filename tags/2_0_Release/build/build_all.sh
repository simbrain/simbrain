sh dist_windows.sh
sh dist_mac.sh
sh dist_linux.sh
rsync -avz -e ssh ./public_html/ simbrain@simbrain.net:public_html/Downloads
