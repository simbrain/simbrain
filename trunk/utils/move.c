/*
 * move.c
 *
 * create comma separated values for object positions
 *
 */
#include<stdio.h>
#define BOTTOM (0)
#define LEFT   (0)
#define HEIGHT (300)
#define WIDTH  (300)
#define TOP    (BOTTOM+HEIGHT)
#define RIGHT  (LEFT+WIDTH)

main()
{
 int j;
 int x=33, y=21;
 int a=1, b=2;

 for( j=0 ; j<10000 ; ++j){
    x += a; 
    y += b; 
    if( LEFT-x>WIDTH || x-RIGHT>WIDTH || BOTTOM-y>HEIGHT || y-TOP>HEIGHT){
      fprintf(stderr,"Position is too far out of bounds\n");
      exit(1);
    } 
    if( x<LEFT ){
      x = 2*LEFT-x;
      a = -a;
    }
    if( x>RIGHT ){
      x = 2*RIGHT-x;
      a = -a;
    }
    if( y<BOTTOM){
      y = 2*BOTTOM-y;
      b = -b;
    }
    if( y>TOP ){
      y = 2*TOP-y;
      b = -b;
    }
    printf("-1, %d, %d\n",x,y);
 }
}
  
 
