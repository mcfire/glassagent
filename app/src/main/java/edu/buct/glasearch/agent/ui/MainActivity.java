package edu.buct.glasearch.agent.ui;

import android.app.Activity;
import android.content.Intent;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import edu.buct.glasearch.agent.R;
import edu.buct.glasearch.agent.callback.CameraCallback;
import edu.buct.glasearch.agent.preview.CameraSurface;

public class MainActivity extends Activity implements CameraCallback{

	//private static final String SERVICE_URL = "http://10.0.2.2:9096/glasearch/search/ms";
    private static final String SERVICE_URL = "http://192.168.1.109:9096/imagesearch/search/ms";
	
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

        //为录音按钮附加事件监听
        takeVoice.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				switch (event.getActionMasked()) {
				case MotionEvent.ACTION_DOWN:
                    //按钮按下时开始录音
					startRecording();
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_CANCEL:
                    //按钮非按下时停止录音
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
        //获取系统的LocationManager定位服务
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        //从定位服务中读取定位信息，设置为每隔5秒最小距离为10米时重新获取
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                10, new LocationListener() {
                    @Override
                    //当地理位置信息发生变化时，记录当前地理位置
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
				String result = search(getImageFileName(), getAudioFileName());
	    		//TODO update ui
				if (result != null) {
                    camerasurface.stopPreview();
		    		Intent intent = new Intent(MainActivity.this, ResultActivity.class);
		    		intent.putExtra("result", result);
		    		startActivity(intent);
				}
	    		return null;
			}
    		
    	};
    	task.execute();    	
    }
    
    private String search(String imageFileName, String voiceFileName) {
        //服务器检索接口的URL
    	String url = SERVICE_URL;
        //准备照片和语音文件
    	File imageFile = new File(imageFileName);
    	File voiceFile = new File(voiceFileName);
    	
    	try {
            //建立HttpPost对象，设置为服务器检索接口的URL
	        HttpPost httpPost = new HttpPost(url);
            //构建Http请求的参数对象
    	    HttpParams params = new BasicHttpParams();
    	    params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
    	    DefaultHttpClient httpClient = new DefaultHttpClient(params);
    	    //使用MultipartEntity存储图像、音频、地理位置三种数据
	        MultipartEntityBuilder builder = MultipartEntityBuilder.create().
	        					setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            //将图像和音频数据加入MultipartEntity
	        builder.addBinaryBody("imageFile", imageFile);
	        builder.addBinaryBody("voiceFile", voiceFile);
	        //如果地理位置信息可用，则将地理位置信息加入MultipartEntity
	        if (currentLocation != null && 
	        	((new Date()).getTime() - currentLocation.getTime()) < 5 * 60 * 1000) {
		        builder.addTextBody("lat", String.valueOf(currentLocation.getLatitude()));
		        builder.addTextBody("lng", String.valueOf(currentLocation.getLongitude()));
	        }
	        //将设置好的数据提交至服务器，并等待服务器返回检索结果，检索结果的格式为JSON
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

    //开始录音
    private void startRecording(){
        //设置录音参数
		recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
						RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
		//开始接收录音数据
		recorder.startRecording();
		isRecording = true;
		//启动工作线程将录音数据写入临时文件
		recordingThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				writeAudioDataToFile();
			}
		},"AudioRecorder Thread");
		recordingThread.start();
	}
	//将录音数据写入文件
	private void writeAudioDataToFile(){
		byte data[] = new byte[bufferSize];
		String filename = getAudioFileName();
		FileOutputStream os = null;
		//打开录音文件
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		int read = 0;
		if(null != os){
            //当前状态为正在录音时
			while(isRecording){
                //从录音器中读取音频数据
				read = recorder.read(data, 0, bufferSize);
				//将数据写入录音文件
				if(AudioRecord.ERROR_INVALID_OPERATION != read){
					try {
						os.write(data);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			//录音完毕关闭文件
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
			//
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
            //打开存放临时照片的位置
			FileOutputStream outStream = new FileOutputStream(getImageFileName());
			//写入照片数据
			outStream.write(data);
			outStream.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		} finally {
            //重新打开拍摄预览
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