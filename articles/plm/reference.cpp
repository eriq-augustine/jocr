/*--------------------------------------------------------------
  Character feature extraction :
     P-LOVE: peripheral local outline vector
   & P-LM: peripheral local moment
    Written by  H.Goto  Dec 2005
--------------------------------------------------------------*/

#include    <stdio.h>
#include    <stdlib.h>
#include    <string.h>
#include    <math.h>

#include    "utypes.h"
#include    "siplib.h"
#include    "xiplib.h"

#include    "feature_PLOVE.h"

class LocalVector{
  public:
    double    v[4];
    LocalVector& operator+=(LocalVector& obj){
        for ( int i=0 ; i<4 ; i++ )  v[i] += obj.v[i];
        return(*this);
      };
    LocalVector& operator/=(double div){
        for ( int i=0 ; i<4 ; i++ )  v[i] /= div;
        return(*this);
      };
    void    copyVector(double *vec, double div){
        for ( int i=0 ; i<4 ; i++ )  *vec++ = v[i] /div;
      };
    void    copyVector3(double *vec, double div){
        for ( int i=0 ; i<3 ; i++ )  *vec++ = v[i] /div;
      };
};


static LocalVector    zeroVector = { 0,0,0,0 };


static void clear_lineFeature(LocalVector **lf){
    int    j,k;
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ )  lf[j][k] = zeroVector;
    }
}


int feature_PLOVE(SIPImage *cimage, FeatureVector *vec){
    OCRPrep    OCRPrep;
    int    i,j,k,x,y,width,height,llen;
    SIPImage    *edge,*exedge;
    uchar    **pc, **pe, **px;
    uchar    c;
    double    sum,*pv,*pvec;
    LocalVector    *fMap[64];
    LocalVector    *lineFeature[8];

    // EQ: Validate 64x64
    if ( cimage->width != 64 && cimage->height != 64 )  return(-1);
    width = height = 64;

    // Set the first line feature as an array of 48 LocalVectors.
    // This is really just a messy way of haning onto the initialized
    // vector witout using an additional variable.
    // The check is just for caution (against out of memory?).
    if ( 0 == (lineFeature[0] = new LocalVector[6*8]) )  return(-1);

    // Initialize each linefeature as an array of 6 LocalVectors.
    for ( i=0 ; i<8 ; i++ )  lineFeature[i] = lineFeature[0] + 6*i;

    // Get an array of LocalVectors, one per pixel.
    if ( 0 == (fMap[0] = new LocalVector[width*height]) ){
        delete []lineFeature[0];
        return(-1);
    }

    // Similar itialization strategy as before.
    // A bit of a messy transformation.
    // Assign the first |height| pixels as LocalVector arrays the size of |width|.
    // This is a messy (but faster) way to init a 2D-array.
    // So fMap is actually treated as a 2D-array.
    for ( y=0 ; y<height ; y++ ){
        fMap[y] = fMap[0] + width * y;
    }

    // Width, Height Depth?
    // No copies, only mallocs?
    if ( NULL == (edge = sip_CreateImage(width, height, 8)) ){
        delete []lineFeature[0];
        delete []fMap[0];
        return(-1);
    }
    if ( NULL == (exedge = sip_CreateImage(width+2, height+2, 8)) ){
        sip_DestroyImage(edge);
        delete []lineFeature[0];
        delete []fMap[0];
        return(-1);
    }

    // Load edge with image info?
    OCRPrep.edge(cimage,edge);
    sip_ClearImage(exedge,0);

    // Point to the pixels?
    pe = (uchar **)edge->pdata;
    px = (uchar **)exedge->pdata;

    // If pe has data, then put a 1 at px one (lower right) over.
    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            if ( pe[y][x] )  px[y+1][x+1] = 1;
        }
    }

    // px just used so no out-of-bounds?
    // For every location in pe, give a directional value.
    // 1: /,  2: |,  3: \,  4: -
    // Why this specific order?
    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            pe[y][x] = c = 0;
            // Our window is actually centered at +1, +1.
            if ( px[y+1][x+1] == 0 )  continue;
            /* 1:/  2:|  3:\  4:- */
            if ( px[y  ][x  ] )  c = 3;
            if ( px[y  ][x+2] )  c = 1;
            if ( px[y+2][x  ] )  c = 1;
            if ( px[y+2][x+2] )  c = 3;
            if ( px[y  ][x+1] )  c = 2;
            if ( px[y+1][x  ] )  c = 4;
            if ( px[y+1][x+2] )  c = 4;
            if ( px[y+2][x+1] )  c = 2;
            pe[y][x] = c;
        }
    }

    // Load px with offset pe data.
    // px is still being used for offset calculations, but now it is just getting other data.
    //  The zeros should be left in there from the first use of px.
    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            px[y+1][x+1] = pe[y][x];
        }
    }

    // For every pixel
    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            // fMap[y][x] is a LocalVecor.
            // v is the data vector double[4] of the LocalVector.
            pv = fMap[y][x].v;

            // Zero out the current LocalVector.
            fMap[y][x] = zeroVector;

            // If in bounds.
            if ( px[y+1][x+1] == 0 )  continue;
            sum = 0;

            // Check 3x3 window with (0,0) at the point.
            for ( j=0 ; j<3 ; j++ ){
                for ( i=0 ; i<3 ; i++ ){
                    // Ensure in bounds
                    if ( (c = px[y+j][x+i]) == 0 )  continue;
                    // Record this direction in this LocalVector's data.
                    pv[(uint)c-1] += 1;

                    // This is a counter, not a sum.
                    sum++;
                }
            }
x
            // Divide the count of each direction by the number of valid pixel's reached.
            // This will usually be 9.
            // Remember, fMap is a 2D-arry of vecors.
            // This will turn each vector value (direction) into the percentage it is used.
            fMap[y][x] /= sum;
        }
    }

    // fMap is now filled with directional inforamtion.

    // Get the real image data.
    pc = (uchar **)cimage->pdata;

    // vec is a FeatureVector.
    // e is a double*.
    pvec = vec->e;

    /* 0deg */
    // j = row, i = col, c = last position value, k = counter.
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<64 ; j++ ){
        // c is the last location.
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            // Just map x = col, y = row.
            x = i;
            y = j;

            // If the last spot is 0 and the current spot is not 0.
            // Moving from bg to character?
            if ( c == 0 && pc[y][x] != 0 ){
                // bucketing rows into groups of 8?
                lineFeature[j/8][k] += fMap[y][x];

                // If this is the third one, go to the next col.
                if ( ++k >= 3 )  break;
            }

            // Set last position.
            c = pc[y][x];
        }

        // This loop is the same as above, except it goes R to L.
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = 63-i;
            y = j;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }

    // Copy the data into pvec (the features).
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector(pvec,8);  pvec += 4;
        }
    }

    /* 45deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<128 ; j++ ){
        c = 0;
        if ( j<64 )  llen = j+1;  else  llen = 128-j;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = i;  y = j-i;
            }
            else{    x = i+j-64;  y = 63-i;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = j-i;  y = i;
            }
            else{    x = 63-i;  y = j+i-64;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector(pvec,16);  pvec += 4;
        }
    }

    /* 90deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<64 ; j++ ){
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = j;
            y = 63-i;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = j;
            y = i;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector(pvec,8);  pvec += 4;
        }
    }

    /* 135deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<128 ; j++ ){
        c = 0;
        if ( j<64 )  llen = j+1;  else  llen = 128-j;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = j-i;  y = 63-i;
            }
            else{    x = 63-i;  y = 127-i-j;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = i;  y = i-j+63;
            }
            else{    x = i+j-64;  y = i;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector(pvec,16);  pvec += 4;
        }
    }

    vec->dim = FVECDIM_PLOVE;

    sip_DestroyImage(exedge);
    sip_DestroyImage(edge);
    delete []lineFeature[0];
    delete []fMap[0];
    return(0);
}


int feature_PLM(SIPImage *cimage, FeatureVector *vec){
    OCRPrep    OCRPrep;
    int    i,j,k,x,y,width,height,llen;
    SIPImage    *edge,*exedge;
    uchar    **pc, **pe, **px;
    uchar    c;
    double    sum,*pv,*pvec;
    double    d,x0,y0,Sx,Sy,Sxy;
    LocalVector    *fMap[64];
    LocalVector    *lineFeature[8];
    if ( cimage->width != 64 && cimage->height != 64 )  return(-1);
    width = height = 64;

    if ( 0 == (lineFeature[0] = new LocalVector[6*8]) )  return(-1);
    for ( i=0 ; i<8 ; i++ )  lineFeature[i] = lineFeature[0] + 6*i;
    if ( 0 == (fMap[0] = new LocalVector[width*height]) ){
        delete []lineFeature[0];
        return(-1);
    }
    for ( y=0 ; y<height ; y++ ){
        fMap[y] = fMap[0] + width * y;
    }
    if ( NULL == (edge = sip_CreateImage(width, height, 8)) ){
        delete []lineFeature[0];
        delete []fMap[0];
        return(-1);
    }
    if ( NULL == (exedge = sip_CreateImage(width+2, height+2, 8)) ){
        sip_DestroyImage(edge);
        delete []lineFeature[0];
        delete []fMap[0];
        return(-1);
    }
    OCRPrep.edge(cimage,edge);
    sip_ClearImage(exedge,0);
    pe = (uchar **)edge->pdata;
    px = (uchar **)exedge->pdata;
    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            if ( pe[y][x] )  px[y+1][x+1] = 1;
        }
    }

    for ( y=0 ; y<height ; y++ ){
        for ( x=0 ; x<width ; x++ ){
            pv = fMap[y][x].v;
            fMap[y][x] = zeroVector;
            if ( px[y+1][x+1] == 0 )  continue;
            sum = 0;
            x0 = y0 = 0;
            for ( j=0 ; j<3 ; j++ ){
                for ( i=0 ; i<3 ; i++ ){
                    if ( px[y+j][x+i] == 0 )  continue;
                    x0 += (double)(i-1);
                    y0 += (double)(j-1);
                    sum++;
                }
            }
            x0 /= sum;
            y0 /= sum;
            Sx = Sy = Sxy = 0;
            for ( j=0 ; j<3 ; j++ ){
                for ( i=0 ; i<3 ; i++ ){
                    if ( px[y+j][x+i] == 0 )  continue;
                    d = (i-1)-x0;
                    Sx += d*d;
                    d = (j-1)-y0;
                    Sy += d*d;
                    Sxy += ((i-1)-x0) * d;
                }
            }
            pv[0] = Sx /sum;
            pv[1] = Sy /sum;
            pv[2] = Sxy /sum;
        }
    }

    pc = (uchar **)cimage->pdata;
    pvec = vec->e;

    /* 0deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<64 ; j++ ){
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = i;
            y = j;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = 63-i;
            y = j;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector3(pvec,8);  pvec += 3;
        }
    }

    /* 45deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<128 ; j++ ){
        c = 0;
        if ( j<64 )  llen = j+1;  else  llen = 128-j;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = i;  y = j-i;
            }
            else{    x = i+j-64;  y = 63-i;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = j-i;  y = i;
            }
            else{    x = 63-i;  y = j+i-64;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector3(pvec,16);  pvec += 3;
        }
    }

    /* 90deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<64 ; j++ ){
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = j;
            y = 63-i;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<64 ; i++ ){
            x = j;
            y = i;
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/8][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector3(pvec,8);  pvec += 3;
        }
    }

    /* 135deg */
    clear_lineFeature(lineFeature);
    for ( j=0 ; j<128 ; j++ ){
        c = 0;
        if ( j<64 )  llen = j+1;  else  llen = 128-j;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = j-i;  y = 63-i;
            }
            else{    x = 63-i;  y = 127-i-j;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
        c = 0;
        for ( i=0, k=0 ; i<llen ; i++ ){
            if ( j<64 ){
                x = i;  y = i-j+63;
            }
            else{    x = i+j-64;  y = i;
            }
            if ( c == 0 && pc[y][x] != 0 ){
                lineFeature[j/16][k+3] += fMap[y][x];
                if ( ++k >= 3 )  break;
            }
            c = pc[y][x];
        }
    }
    for ( j=0 ; j<8 ; j++ ){
        for ( k=0 ; k<6 ; k++ ){
            lineFeature[j][k].copyVector3(pvec,16);  pvec += 3;
        }
    }

    vec->dim = FVECDIM_PLM;

    sip_DestroyImage(exedge);
    sip_DestroyImage(edge);
    delete []lineFeature[0];
    delete []fMap[0];
    return(0);
}


