/*
 * move.c
 *
 * create comma separated values for object positions
 *
 */
#include<stdio.h>
#define BOTTOM (0)
#define LEFT   (0)
#define HEIGHT (180)
#define WIDTH  (360)
#define TOP    (BOTTOM+HEIGHT)
#define RIGHT  (LEFT+WIDTH)

main()
{
 int j;
 int  x1=WIDTH/2, y1=10;
 int  x2=WIDTH/2, y2=280;
 int  v1=1, v2=2;

 printf("0, %d, %d\n", WIDTH/2, HEIGHT/2);

 for( j=0 ; j<10000 ; ++j){

    printf("1, %d, %d\n",x1,y1);
    printf("2, %d, %d\n",x2,y2);

    y1 = (y1 + v1) % HEIGHT; 
    y2 = (y2 + v2) % HEIGHT;
 }
}
  
 
