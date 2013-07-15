package com.holoware.aktp;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import uk.co.senab.photoview.PhotoViewAttacher;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.ProgressBar;

public class MainActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener {
	
	private static final int REQUEST_CODE = 100;
	private static final int ORIGINAL = 0;
	private static final int HISTOGRAM = 1;
	private static final int GRAYSCALE = 2;
	private static final int THRESHOLD = 3;
	private static final int SEPIA = 4;
	private static final int FILTER = 5;
	private static final int CONTOUR = 6;
	private static final int MOSAIC = 7;
	
	private int currentPosition;
	private Bitmap bitmap;
	private Bitmap originalBitmap;
	private ImageView mImageView;
	private PhotoViewAttacher mAttacher;

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Restore bitmaps
		Bitmap[] savedBmps = (Bitmap[])getLastCustomNonConfigurationInstance();
		if (savedBmps != null){
			originalBitmap = savedBmps[0];
			bitmap = savedBmps[1];
		}
				
		mImageView = (ImageView)findViewById(R.id.imageView1);
		mAttacher = new PhotoViewAttacher(mImageView);		

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.original),
								getString(R.string.histogram),
								getString(R.string.grayscale),
								getString(R.string.threshhold),
								getString(R.string.sepia),
								getString(R.string.filter),
								getString(R.string.contour),
								getString(R.string.mosaic) }), this);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			currentPosition = savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM);
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));			
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());	
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		Bitmap[] bmps = new Bitmap[]{originalBitmap, bitmap};
		return bmps;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id) {		
		switch (position){
		case ORIGINAL:
			if (originalBitmap == null)
				new ImageTask(ORIGINAL).execute();
			else {				
				mImageView.setImageBitmap(originalBitmap);
				if (mAttacher.getScaleType() != ScaleType.CENTER_INSIDE){
					mAttacher.setScaleType(ScaleType.CENTER_INSIDE);
				}
				mAttacher.update();
			}			
			break;
			
		default:		
			ScaleType desiredScaleType;
			if (position == HISTOGRAM)
				desiredScaleType = ScaleType.FIT_XY;
			else
				desiredScaleType = ScaleType.CENTER_INSIDE;
				
			if ((bitmap != null) & (currentPosition == position)){
				mImageView.setImageBitmap(bitmap);
				if (mAttacher.getScaleType() != desiredScaleType){
					mAttacher.setScaleType(desiredScaleType);
					mAttacher.update();
				}
			} else {				
				new ImageTask(position).execute();
			}
			
			currentPosition = position;
			break;
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
		case R.id.open_image:
			Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("image/");
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			startActivityForResult(intent, REQUEST_CODE);
			break;		
		}
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE && resultCode == RESULT_OK){
			try {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                
                originalBitmap = decodeStream(data.getData()); 
                
                currentPosition = -1;
                
                if (getActionBar().getSelectedNavigationIndex() != ORIGINAL)
                	getActionBar().setSelectedNavigationItem(ORIGINAL);
                else {
                	mImageView.setImageBitmap(originalBitmap);
                    mAttacher.update();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
		}
	}
	
	private Bitmap decodeStream(Uri uri) throws IOException{
		InputStream stream = getContentResolver().openInputStream(uri);
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth(); 
		int height = display.getHeight();  
		int REQUIRED_SIZE = Math.max(width, height);
		
          //decode image size
		BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(stream, null, o);              
        
        //Find the correct scale value. It should be the power of 2.
        int width_tmp=o.outWidth;
        int height_tmp=o.outHeight;
        stream.close();
        int scale=1;
        while(true){
        	if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
        		break;
        	width_tmp/=2;
        	height_tmp/=2;
        	scale*=2;
        }
        
        //decode with inSampleSize
        stream = getContentResolver().openInputStream(uri);
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize=scale;
        Bitmap result = BitmapFactory.decodeStream(stream, null, o2);
        stream.close();
        return result;
    }


	public class ImageTask extends AsyncTask<Void, Void, Bitmap>{
		private int operation;
		
		public ImageTask (int operation){
			super();
			this.operation = operation;
		}
		
		@Override
		protected void onPreExecute() {
			final ProgressBar progress = (ProgressBar)findViewById(R.id.progressBar1);
			if (progress.getVisibility() == View.INVISIBLE)
				progress.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(Void... args) {	
			Bitmap bitmap = null;
			
			switch (operation){
			case ORIGINAL:
				bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.popeye);
				break;
				
			case HISTOGRAM:
				bitmap = ImageUtils.getHistogram(originalBitmap);
				break;	
				
			case GRAYSCALE:
				bitmap = ImageUtils.toGrayscale(originalBitmap);
				break;
				
			case THRESHOLD:
				bitmap = ImageUtils.toThreshold(originalBitmap);
				break;	
				
			case SEPIA:
				bitmap = ImageUtils.toSepia(originalBitmap);
				break;	
				
			case FILTER:
				bitmap = ImageUtils.applyFilter(originalBitmap);
				break;		
				
			case CONTOUR:
				bitmap = ImageUtils.getContour(originalBitmap);
				break;	
				
			case MOSAIC:
				bitmap = ImageUtils.getMosaic(originalBitmap);
				break;
			}
			
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			final ProgressBar progress = (ProgressBar)findViewById(R.id.progressBar1);
			ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(progress, View.ALPHA, 0);
			alphaAnimator.setDuration(500);
			alphaAnimator.start();
			alphaAnimator.addListener(new AnimatorListener(){

				@Override
				public void onAnimationCancel(Animator arg0) {					
				}

				@Override
				public void onAnimationEnd(Animator animation) {
					progress.setVisibility(View.INVISIBLE);
					progress.setAlpha(1);
				}

				@Override
				public void onAnimationRepeat(Animator animation) {					
				}

				@Override
				public void onAnimationStart(Animator animation) {					
				}
				
			});
			
			mImageView.setImageBitmap(result);
			if (operation == HISTOGRAM)
				mAttacher.setScaleType(ScaleType.FIT_XY);
			else
				mAttacher.setScaleType(ScaleType.CENTER_INSIDE);
			mAttacher.update();
			   
			
			switch (operation){
			case ORIGINAL:
				originalBitmap = result;
				break;
				
			default:
				if (bitmap != null)
					bitmap.recycle();
				bitmap = result;
				break;
			}
		
		}

	};
}
