package com.varma.samples.camera.ui;

import java.io.FileOutputStream;

import android.app.Activity;
import android.app.AlertDialog;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.varma.samples.camera.R;
import com.varma.samples.camera.callback.CameraCallback;
import com.varma.samples.camera.preview.CameraSurface;

public class MainActivity extends Activity implements CameraCallback{
	private FrameLayout cameraholder = null;
	private CameraSurface camerasurface = null;
	private ImageButton takePicture = null;
	private ImageButton takeVoice = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        cameraholder = (FrameLayout)findViewById(R.id.camera_preview);
        takeVoice = (ImageButton)findViewById(R.id.takeVoice);
        takePicture = (ImageButton)findViewById(R.id.takepicture);
        
        setupPictureMode();
        
        takeVoice.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					startRecord();
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					stopRecord();
					break;
				}
				return false;
			}
        	
        });
        
        takePicture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				camerasurface.takePicture();
			}
        	
        });
        
    }
    
    private void startRecord() {
    	
    }
    
    private void stopRecord() {
    	
    }
    
    private void setupPictureMode(){
    	camerasurface = new CameraSurface(this);
    	
    	cameraholder.addView(camerasurface, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    	
    	camerasurface.setCallback(this);
    }
    
	@Override
	public void onJpegPictureTaken(byte[] data, Camera camera) {
		try
		{
			FileOutputStream outStream = new FileOutputStream(String.format(
					Environment.getExternalStorageDirectory().getPath() + "%d.jpg", 
					System.currentTimeMillis()));
			
			outStream.write(data);
			outStream.close();
			camerasurface.startPreview();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
	}

	@Override
	public void onRawPictureTaken(byte[] data, Camera camera) {
	}

	@Override
	public void onShutter() {
	}
	
	@Override
	public String onGetVideoFilename(){
		String filename = String.format("/sdcard/%d.3gp",System.currentTimeMillis());
		
		return filename;
	}
	
	private void displayAboutDialog()
	{
    	final AlertDialog.Builder builder = new AlertDialog.Builder(this);

    	builder.setTitle(getString(R.string.app_name));
    	builder.setMessage("Sample application to demonstrate the use of Camera in Android\n\nVisit www.krvarma.com for more information.");
    	
    	builder.show();
	}
}