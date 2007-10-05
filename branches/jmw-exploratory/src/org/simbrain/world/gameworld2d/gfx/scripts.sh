#giftrans -t '#0099ff' <bubble.gif >bubble-tr.gif

#pngtopnm <bubble16.png | ppmquant 8 -fs | ppmtogif | giftrans -t '#0099ff' >bubble16-tr.gif


#giftopnm -image=all Fire-bar.gif  | pnmsplit
#pnmcat -leftright image? | xv -



#giftopnm plasmabullet-37x20.gif | pnmresample -x 64 -y 9 | ppmquant 256 -fs | ppmtogif | giftrans -t '#000' >plasmabullet-16x9.gif
