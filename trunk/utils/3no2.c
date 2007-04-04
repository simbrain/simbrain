/*
 * 3no2.c
 *
 * create comma separated values for the positions 
 * of three objects as each is translated on a torus
 * no two objects can be near the creature at the same time.
 *
 */
#include<stdio.h>
#include<stdlib.h>
#define HEIGHT (360)
#define WIDTH  (360)

main()
{
 int j;
 int x1=80, y1=100, u1=0, v1=1;
 int x2=80, y2=130, u2=0, v2=1;
 int x3=80, y3=160, u3=0, v3=1;
 
 printf("0, 80, 180\n");
 for( j=0 ; j<360 ; ++j){
    printf("1, %d, %d\n",x1,y1);
    x1 = (x1+u1) % WIDTH; 
    if( x1<0 ) x1+=360;
    y1 = (y1+v1) % HEIGHT;
    if( y1<0 ) y1+=360;
    printf("2, %d, %d\n",x2,y2);
    x2 = (x2+u2) % WIDTH; 
    if( x2<0 ) x2+=360;
    y3 = (y3+v3) % HEIGHT;
    if( y2<0 ) y2+=360;
    printf("3, %d, %d\n",x3,y3);
    x3 = (x3+u3) % WIDTH; 
    if( x3<0 ) x3+=360;
    y2 = (y2+v2) % HEIGHT;
    if( y3<0 ) y3+=360;
    if( ( (x1-180)*(x1-180)+(y1-180)*(y1-180) < 3600 &&
          (x2-180)*(x2-180)+(y2-180)*(y2-180) < 3600  ) ||
        ( (x1-180)*(x1-180)+(y1-180)*(y1-180) < 3600 &&
          (x3-180)*(x3-180)+(y3-180)*(y3-180) < 3600  ) ||
        ( (x2-180)*(x2-180)+(y2-180)*(y2-180) < 3600 &&
          (x3-180)*(x3-180)+(y3-180)*(y3-180) < 3600  )  ){
      fprintf(stderr,"made it to %d\n",j);
      fprintf(stderr,"( %d, %d), ( %d, %d), ( %d, %d)\n",x1,y1,x2,y2,x3,y3);
      break;
   }
 }
}
