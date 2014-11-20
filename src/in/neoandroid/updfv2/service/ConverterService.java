package in.neoandroid.updfv2.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.xmlpull.v1.XmlPullParserException;

import in.neoandroid.updfv2.Util;
import in.neoandroid.updfv2.database.QueueDB;
import in.neoandroid.updfv2.database.QueueDB.FileDetails;
import in.neoandroid.xmlparser.NeoXMLObject;
import in.neoandroid.xmlparser.NeoXMLParser;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class ConverterService extends Service {
	private final static String SERVER = "SOMESERVER.com";
	private final static String SERVER_URL = "http://"+SERVER+"/updf/";
	private final static String UPLOAD_URL = SERVER_URL+"upload.php";
	private final static String DOWNLOAD_URL = SERVER_URL+"getfile.php";
	private QueueDB queuedb = QueueDB.getInstance(this);

	private void updateStatus(FileDetails details, int status, String sStatus) {
		details.status = status;
		details.server_status = sStatus;
		queuedb.updateStatus(details);
	}

	private void fileNotFound(FileDetails details) {
		updateStatus(details, FileDetails.STATUS.ERROR.ordinal(), "File Not Found!");
	}

	public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName(SERVER);
            if(ipAddr != null && ipAddr.getHostAddress().length() > 0)
                return true;
        } catch (Exception e) {  }
        return false;
    }
	
	private File getFileToWrite(FileDetails details) {
		File file = null;
		if(Util.prefGetSameDirectorySave(this)) {
			file = new File(Util.joinFilename(details.path, details.filename, "pdf"));
			if(!file.exists() && file.getParentFile().canWrite())
				return file;
		}
		file = new File(Util.joinFilename(Util.prefGetDefaultDirectory(this), details.filename, "pdf"));
		if(!file.exists() && file.getParentFile().canWrite())
			return file;
		return null; // None found!
	}
	
	private void saveFile(FileDetails details, HttpResponse response) throws IOException {
		File outFile = getFileToWrite(details);
		if(outFile == null) {
			updateStatus(details, FileDetails.STATUS.ERROR.ordinal(), "Save Failed!");
			return;
		}
		HttpEntity entity = response.getEntity();
		FileOutputStream fos = new FileOutputStream(outFile);
		entity.writeTo(fos);
		entity.consumeContent();
		fos.flush();
		fos.close();
		details.saved_as = outFile.getAbsolutePath();
		Log.i("[UPDFBackground]", "Converted: "+details.saved_as);
		updateStatus(details, FileDetails.STATUS.CONVERTED.ordinal(), "");
	}
	
	private void parseAndUpdate(FileDetails details, HttpResponse response) throws IllegalStateException, IOException, XmlPullParserException {
		String header = response.getFirstHeader("Content-type").getValue();
		if(header == null || !header.contains("xml")) {
			saveFile(details, response);
			return;
		}
		NeoXMLParser parser = new NeoXMLParser(response.getEntity().getContent());
		NeoXMLObject xmlObj = parser.parse();
		NeoXMLObject errorObj = xmlObj.getFirstChild("error");
		NeoXMLObject fileid = xmlObj.getFirstChild("fileid");
		NeoXMLObject status = xmlObj.getFirstChild("status");
		if(errorObj != null) {
			updateStatus(details, FileDetails.STATUS.ERROR.ordinal(), errorObj.getContent());
			return;
		}
		if(fileid != null && fileid.getContent() != null)
			details.server_fileId = fileid.getContent();
		if(status != null && status.getContent() != null) {
			String sStatus = "Processing";
			if(!status.getContent().contentEquals("0"))
				sStatus = "Queue: "+status.getContent();
			updateStatus(details, FileDetails.STATUS.QUEUED.ordinal(), sStatus);
		}
	}
	
	private boolean uploadOrProcessFile(FileDetails details) {
		boolean uploadFile = (details.server_fileId == null);
		HttpClient client = new DefaultHttpClient();
    	HttpPost httppost = new HttpPost(uploadFile ? UPLOAD_URL : DOWNLOAD_URL);
    	MultipartEntity entity = new MultipartEntity();
		try {
			if(uploadFile) {
				String fullPath = Util.joinFilename(details.path, details.filename, details.ext);
				FileBody fileBody = new FileBody(new File(fullPath));
				entity.addPart("userfile", fileBody);
				entity.addPart("MAX_FILE_SIZE", new StringBody("5000000"));
				Log.i("[UPDFBackground]", "Uploading: "+fullPath);
			} else {
				entity.addPart("fileid", new StringBody(details.server_fileId));
			}
			entity.addPart("id", new StringBody(Util.prefGetDeviceID(this)));
			httppost.setEntity(entity);
			HttpResponse response = client.execute(httppost);
			parseAndUpdate(details, response);
		} catch(FileNotFoundException e) {
			fileNotFound(details);
			return false;
		} catch(Exception e) { e.printStackTrace(); return false; }
		return true;
	}

  	private Thread mThread = new Thread() { 
  		@Override
  	    public void run() {
  			while(true) {
  				try {
  					FileDetails details = null;
					if(!isInternetAvailable() || (details = queuedb.getFirstFile()) == null) {
						sleep(30000);
						continue;
					}

					// Do the conversion
					while(details.status == FileDetails.STATUS.QUEUED.ordinal()) {
						sleep(15000);	// Sleep 15sec
						uploadOrProcessFile(details);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
  			}
			stopSelf();
  		}
  	};
  	
  	public static void startService(Activity activity) {
  		Intent intent = new Intent(activity, ConverterService.class);
  		activity.startService(intent);
  	}
  	
  	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("[UPDFBackground]", "Received start id " + startId + ": " + intent);
        if(!mThread.isAlive())
        	mThread.start();
        return START_STICKY; // Continue running
    }

    @Override
    public void onDestroy() {
        Log.i("[UPDFBackground]", "Service Stopped");
    }

	@Override
	public IBinder onBind(Intent intent) {
		return null; // Doesn't support binding, so return null
	}
}
