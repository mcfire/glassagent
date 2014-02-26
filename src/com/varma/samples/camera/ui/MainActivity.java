package com.varma.samples.camera.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
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

	private static final String SERVICE_URL = "http://10.0.2.2:9096/glasearch/search/ms";
	
	private static final int RECORDER_SAMPLERATE = 8000;
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	private FrameLayout cameraholder = null;
	private CameraSurface camerasurface = null;
	private ImageButton takePicture = null;
	private ImageButton takeVoice = null;
	private ImageButton search = null;
	
	private AudioRecord recorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private boolean isRecording = false;
	private Location currentLocation = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        cameraholder = (FrameLayout)findViewById(R.id.camera_preview);
        takeVoice = (ImageButton)findViewById(R.id.takeVoice);
        takePicture = (ImageButton)findViewById(R.id.takepicture);
        search = (ImageButton)findViewById(R.id.search);
        
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);
        
        setupPictureMode();
        
        takeVoice.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
					startRecording();
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
					stopRecording();
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
        
        search.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				doSearch();
			}
        	
        });
        
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                10, new LocationListener() {
                    @Override
                    public void onLocationChanged(final Location location) {
                    	currentLocation = new Location(location);
                    }

        			@Override
        			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        				
        			}

        			@Override
        			public void onProviderDisabled(String arg0) {}

        			@Override
        			public void onProviderEnabled(String arg0) {}
                });
    }
    
    private void doSearch() {
    	AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... params) {
				return upload(params[0], params[1]);
			}
    		
    	};

    	task.execute(getImageFileName(), getAudioFileName());
    }
    
    private String upload(String imageFileName, String voiceFileName) {
    	String url = SERVICE_URL;
    	File imageFile = new File(imageFileName);
    	File voiceFile = new File(voiceFileName);
    	
    	try {

	        HttpPost httpPost = new HttpPost(url);

    	    HttpParams params = new BasicHttpParams();
    	    params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    	    DefaultHttpClient httpClient = new DefaultHttpClient(params);
    	    
	        MultipartEntityBuilder builder = MultipartEntityBuilder.create().
	        					setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
	        builder.addBinaryBody("imageFile", imageFile);
	        builder.addBinaryBody("voiceFile", voiceFile);
	        
	        if (currentLocation != null && 
	        	((new Date()).getTime() - currentLocation.getTime()) < 5 * 60 * 1000) {
		        builder.addTextBody("lat", String.valueOf(currentLocation.getLatitude()));
		        builder.addTextBody("lng", String.valueOf(currentLocation.getLongitude()));
	        }
	   
	        httpPost.setEntity(builder.build());

	        String result = httpClient.execute(httpPost, new ResponseHandler<String>() {

				@Override
				public String handleResponse(HttpResponse response)
						throws ClientProtocolException, IOException {
					return EntityUtils.toString(response.getEntity());
				}
	        	
	        });
	        return result;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
    	return null;
    }
    
    private void startRecording(){
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
						RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
		
		recorder.startRecording();
		
		isRecording = true;
		
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				writeAudioDataToFile();
			}
		},"AudioRecorder Thread");
		
		recordingThread.start();
	}
	
	private void writeAudioDataToFile(){
		byte data[] = new byte[bufferSize];
		String filename = getAudioFileName();
		FileOutputStream os = null;
		
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		int read = 0;
		
		if(null != os){
			while(isRecording){
				read = recorder.read(data, 0, bufferSize);
				
				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void stopRecording(){
		if(null != recorder){
			isRecording = false;
			
			recorder.stop();
			recorder.release();
			
			recorder = null;
			recordingThread = null;
		}
	}
	
	private String getAudioFileName() {
		return getAppFolder() + "voice.raw";
	}
	
	private String getImageFileName() {
		return getAppFolder() + "image.jpg";
	}
	
	private String getAppFolder() {
		File folder = new File(getApplicationContext().getFilesDir(), "/glassagent/");
		if(!folder.exists()){
			folder.mkdirs();
		}
		return folder.getAbsolutePath() + "/";
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
			FileOutputStream outStream = new FileOutputStream(getImageFileName());
			
			outStream.write(data);
			outStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		} finally {
			camerasurface.startPreview();
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
}