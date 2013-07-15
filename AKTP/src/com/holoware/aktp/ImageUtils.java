package com.holoware.aktp;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;

/**
 * Contains static methods for image processing*/
public class ImageUtils {
	public static int STEP = 14;//Size of mosaic region (14 X 14)
	
	
	//Histogram 
	public static Bitmap getHistogram(Bitmap bmpOriginal){
		
		//Scale bmpOriginal to improve performance 
		final int dstWidth = 200;
		int dstHeight = dstWidth*bmpOriginal.getHeight()/bmpOriginal.getWidth();		
		Bitmap bmpScaled = Bitmap.createScaledBitmap(bmpOriginal, dstWidth, dstHeight, false);
		int[] histogramValues = new int[256];
		
		int[] pixels = new int[dstWidth];
		int pxBrightness;
		for (int row = 0; row < dstHeight; row++){
			bmpScaled.getPixels(pixels, 0, dstWidth, 0, row, dstWidth, 1);
			for (int col = 0; col < dstWidth; col++){
				pxBrightness = rgbToGray(pixels[col]);
				histogramValues[pxBrightness]++;
			}
		}		
		bmpScaled.recycle();
		
		int histogramMax = max(histogramValues);
        Bitmap bmpHistogram = Bitmap.createBitmap(256, histogramMax, Config.ARGB_8888);
        Canvas canvas = new Canvas(bmpHistogram);
        Paint paint = new Paint();
        paint.setColor(Color.CYAN);
        
        for (int i=0; i<256; i++)
        	canvas.drawLine(i, histogramMax-histogramValues[i], i, histogramMax, paint);
		       
		return bmpHistogram;
	}

	//Grayscale
	public static Bitmap toGrayscale(Bitmap bmpOriginal){        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    ColorMatrix cm = new ColorMatrix();
	    cm.setSaturation(0);
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
	    paint.setColorFilter(f);
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    return bmpGrayscale;
	}
	
	//Sepia
	public static Bitmap toSepia(Bitmap bmpOriginal){        
	    int width, height;
	    height = bmpOriginal.getHeight();
	    width = bmpOriginal.getWidth();    

	    Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	    Canvas c = new Canvas(bmpGrayscale);
	    Paint paint = new Paint();
	    
	    ColorMatrix grMatrix = new ColorMatrix();
	    grMatrix.setSaturation(0);
	    
	    ColorMatrix scMatrix = new ColorMatrix();
	    scMatrix.setScale(1f, .85f, .72f, 1.0f);
	    grMatrix.setConcat(scMatrix, grMatrix);
	    
	    ColorMatrixColorFilter f = new ColorMatrixColorFilter(grMatrix);
	    paint.setColorFilter(f);
	    c.drawBitmap(bmpOriginal, 0, 0, paint);
	    return bmpGrayscale;
	}
	
	//Threshold 
		public static Bitmap toThreshold(Bitmap bmpOriginal){
			int imageWidth = bmpOriginal.getWidth();
			int imageHeight = bmpOriginal.getHeight();
			Bitmap bmpThreshold = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.RGB_565);
			int[] buffer = new int[imageWidth];
			
			int grayVal;
			for (int row = 0; row < imageHeight; row++){
				for (int col = 0; col < imageWidth; col++){
					grayVal = rgbToGray(bmpOriginal.getPixel(col, row));
					if (grayVal > 125)
						buffer[col] = Color.rgb(255, 255, 255);
					else
						buffer[col] = Color.rgb(0, 0, 0);
				}
				bmpThreshold.setPixels(buffer, 0, imageWidth, 0, row, imageWidth, 1);
			}
			return bmpThreshold;
		}
	
	//Low frequency filter
	public static Bitmap applyFilter(Bitmap bmpOriginal){
		int width = bmpOriginal.getWidth();
		int height = bmpOriginal.getHeight();
		
		int core[][]={{1, 1, 1},
					 { 1, 2, 1},
					 { 1, 1, 1}};
		
		int[] result = new int[height*width];
		int[][] pixels2D = new int[height][width];
		for (int i=0; i<height; i++){
			bmpOriginal.getPixels(pixels2D[i], 0, width, 0, i, width, 1);
		}
		
		int count = 0;
		int R,G,B;
		for (int i = 0; i < height; i++)
			for (int j = 0; j<width; j++){	
				R = dotProduct(i, j, 0, pixels2D, core)/10+20;
				G = dotProduct(i, j, 1, pixels2D, core)/10+20;
				B = dotProduct(i, j, 2, pixels2D, core)/10+20;
				
				result[count] = Color.rgb(R, G, B);
				count++;
		}
		
		Bitmap bmpFiltered = Bitmap.createBitmap(result, 0, width, width, height, Bitmap.Config.RGB_565);
		return bmpFiltered;
	}
	
	//Contour
	/**
	 * Forms the outline of the image, using Prewitt operator 
	 * http://en.wikipedia.org/wiki/Prewitt_operator*/
	public static Bitmap getContour(Bitmap bmpOriginal){
		int width = bmpOriginal.getWidth();
		int height = bmpOriginal.getHeight();
		
		int core1[][]={{-1, -1, -1},
					   { 0,  0,  0},
					   { 1,  1,  1}};
	
		int core2[][]={{-1, 0,  1},
		           	   {-1, 0,  1},
		           	   {-1, 0,  1}};
		
		Bitmap bmpGrayscale = toGrayscale(bmpOriginal);
		int[] result = new int[height*width];
		int[][] pixels2D = new int[height][width];
		for (int i=0; i<height; i++){
			bmpGrayscale.getPixels(pixels2D[i], 0, width, 0, i, width, 1);
		}
		
		int count = 0;
		int dp;
		int w, h;
		for (int i = 0; i < height; i++)
			for (int j = 0; j<width; j++){	
				w = dotProduct(i, j, 1, pixels2D, core1)/8;
				h = dotProduct(i, j, 1, pixels2D, core2)/8;
				dp = (int) Math.sqrt(Math.pow(w, 2)*Math.pow(h, 2));
				dp=(dp>255)?255:dp;
				result[count] = Color.rgb(dp, dp, dp);
				count++;
		}	
		bmpGrayscale.recycle();
		Bitmap bmpFiltered = Bitmap.createBitmap(result, 0, width, width, height, Bitmap.Config.RGB_565);
		return bmpFiltered;
	}
	
	//Mosaic
	public static Bitmap getMosaic(Bitmap bmpOriginal){	
			
		int width = bmpOriginal.getWidth();
		int height = bmpOriginal.getHeight();		
			
		int sizeW = width/STEP;
		int sizeH = height/STEP;
			
		int resultW = sizeW*STEP;
		int resultH = sizeH*STEP;

		Bitmap bmpMosaic = Bitmap.createBitmap(resultW, resultH, Bitmap.Config.RGB_565);
			
		int col;		
		for (int i=0; i < sizeH; i++)
			for (int j=0; j<sizeW; j++){
				col = computeAverageColor(j, i, bmpOriginal);
				fillRigeon(col, j, i, bmpMosaic);
			}
		
		return bmpMosaic;
	}
	
	
	/***********************************Helper methods*******************************************************/
	
	static int rgbToGray(int rgb) {	
		int r = (rgb >> 16) & 0xff;
		int g = (rgb >> 8) & 0xff;
		int b = rgb & 0xff;
		int k = (int)(.56*g+.33*r+.11*b);
		return k;
	}
	
	/**
	 * @param x X coordinate of a pixel
	 * @param y Y coordinate of a pixel
	 * @param col Tone (R-0, G-1, B-2)  
	 * @param pixels2D Pixel matrix
	 * @param core Core
	 * @return pixel tone value, calculated in accordance with the core
     * */
	static int dotProduct(int x, int y, int col, int[][] pixels2D, int[][] core){
		int sum = 0;
		
		int px = 1;
		for (int i = -1; i < 1; i++)
			for (int j = -1; j < 1; j++){
				if ((x+i<0)||(y+j<0))
					px = 1;
				else {
					switch (col){
					case 0: //red
						px = Color.red(pixels2D[x+i][y+j]);
						break;
							
					case 1: //green
						px = Color.green(pixels2D[x+i][y+j]);
						break;
							
					case 2: //blue
						px = Color.blue(pixels2D[x+i][y+j]);
						break;	
					}
				}
				sum = sum + core[i+1][j+1]*px;
			}
		return sum;
	}
	
	/**
	 * @param x X coordinate of the region
	 * @param y Y coordinate of the region
	 * @param bmp Source bitmap
	 * @return color value of the region
     * */
	static int computeAverageColor(int x, int y, Bitmap bmp){
		int res[] = new int[3];	
		for (int col=0; col<3; col++){
			for (int i=x*STEP; i < (x+1)*STEP; i++)
				for (int j=y*STEP; j < (y+1)*STEP; j++){
					switch (col){
					case 0: //red
						res[0] = res[0] + Color.red(bmp.getPixel(i, j));
						break;
							
					case 1: //green
						res[1] = res[1] + Color.green(bmp.getPixel(i, j));
						break;
							
					case 2: //blue
						res[2] = res[2] + Color.blue(bmp.getPixel(i, j));
						break;	
					}
				}
			res[col] = res[col]/(STEP*STEP);
		}
		
		return Color.rgb(res[0], res[1], res[2]);
	}
	
	/**
	 * @param color Color of the region
	 * @param x X coordinate of the region
	 * @param y Y coordinate of the region
	 * @param bmp Source bitmap
     * */
	static void fillRigeon(int color, int x, int y, Bitmap bmp){
		
		for (int i=x*STEP; i < (x+1)*STEP; i++)
			for (int j=y*STEP; j < (y+1)*STEP; j++){
				bmp.setPixel(i, j, color);
			}
	}
	
	static int max(int[] t) {
	    int maximum = t[0];   
	    for (int i=1; i<t.length; i++) {
	        if (t[i] > maximum) {
	            maximum = t[i];   
	        }
	    }
	    return maximum;
	}
}