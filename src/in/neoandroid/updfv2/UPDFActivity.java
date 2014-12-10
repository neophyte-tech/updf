package in.neoandroid.updfv2;

import in.neoandroid.neofilepicker.NeoFilePicker;
import in.neoandroid.updfv2.database.QueueDB;
import in.neoandroid.updfv2.service.ConverterService;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Pair;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

public class UPDFActivity extends ActionBarActivity {
	private String[] menu_array;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private ScrollView svMainContent;
	private UPDFScreen screens[] = null;
	private final static int FILE_PICKER_REQUEST = 1;
	private final static int DIR_PICKER_REQUEST = 2;
	public QueueDB queuedb = null;
	private DIRPickCB dirPickCallback = null;
	public UPDFScreen currentScreen = null;
	private InterstitialAd interstitial = null;

	private enum SCREENS {
		HOME,
		PICK_FILE_TO_CONVERT,
		CONVERTED_PDFS,
		QUEUED_FILES,
		SETTINGS,
		RATE_APP,
		SHARE_APP
	};
	
	public interface UPDFScreen {
		public void activate();
	};
	
	public interface DIRPickCB {
		public void dirPickCallback(String dir);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_ACTION_BAR);

		setContentView(R.layout.updf_layout);

		final android.support.v7.app.ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.ultimate_pdf_conv);
        menu_array = getResources().getStringArray(R.array.menuArray);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.menu_row, menu_array));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        	actionBar.setHomeButtonEnabled(true);
        }


		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
				R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {
            public void onDrawerClosed(View view) {
            	actionBar.setTitle(R.string.ultimate_pdf_conv);
                //invalidateOptionsMenu(); 
            }

            public void onDrawerOpened(View drawerView) {
            	actionBar.setTitle(R.string.drawer_open);
                //invalidateOptionsMenu(); 
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        // Initialize DeviceId
        initializeDeviceID();

        // Set Database
        queuedb = QueueDB.getInstance(this);
        
        // Set Views
		svMainContent = (ScrollView) findViewById(R.id.svMainContent);
        
        /// Initialize all screens - should be in the order of menu
        screens = new UPDFScreen[] {
			new HomeScreen(this, svMainContent),
			null, // Convert/Call FilePicker
			new QueuedFilesScreen(this, svMainContent, true),
			new QueuedFilesScreen(this, svMainContent, false),
			new SettingsScreen(this, svMainContent),
			null,   // Rate App
			null	// Share App
        };
        
        // Start HomeScreen
        screens[SCREENS.HOME.ordinal()].activate();
        
        // Start Converter Service
        ConverterService.startService(this);
        
        // Check and queue from Intent
        parseIntent(getIntent());
        
        // Banner Ads
        AdView adView = (AdView)findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());
        
        // Interstitial Ads
        interstitial = new InterstitialAd(this);
        interstitial.setAdUnitId(getResources().getString(R.string.admob_id2));

        // Begin loading your interstitial.
        interstitial.loadAd(new AdRequest.Builder().build());
	}
	
	private void displayInterstitial() {
	    if(interstitial != null && interstitial.isLoaded())
	    	interstitial.show();
	}
	
	private String getRealPathFromURI(Uri contentUri) {
        String [] media={MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri,
                        media, // Which columns to return
                        null,
                        null,
                        null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
	}
	
	private void parseIntent(Intent intent) {
		if(intent != null && intent.getExtras() != null) {
        	String name = null;
        	try {
        		name = getRealPathFromURI((Uri)intent.getExtras().getParcelable(Intent.EXTRA_STREAM));
        	}
        	catch(Exception e) { name = null; }
        	try {
        		if(name == null)
        			name = ((Uri)intent.getExtras().getParcelable(Intent.EXTRA_STREAM)).getPath();
        	} catch(Exception e) { name = null; }

        	if(name != null)
        		queueFileAndShow(new File(name));
		}
	}
	
	private String getNewDeviceId() {
    	String deviceID = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);
        if(deviceID == null)
        	deviceID = "123456"+String.valueOf((new Random()).nextLong());
        return deviceID;
    }
	
	private void initializeDeviceID() {
		if(Util.prefGetDeviceID(this).length() <= 0)
			Util.prefSetDeviceID(this, getNewDeviceId());
	}
	
	/**
	 * Creating options menu.
	 * Currently unused.
	 */
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_main, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	/**
	 * Prepare Options menu.
	 * Currently disabled.
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
	    //MenuItem item= menu.findItem(R.id.menu_settings);
	    return false; // false => Menu is disabled
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	if(keyCode == KeyEvent.KEYCODE_MENU) {
    		mDrawerLayout.openDrawer(Gravity.LEFT);
    		return true;
	    }
	    return super.onKeyDown(keyCode, event);
    }
    
    private void rateApp() {
    	final Uri uri = Uri.parse("market://details?id="+getPackageName());
    	final Intent rateAppIntent = new Intent(Intent.ACTION_VIEW, uri);

    	if (getPackageManager().queryIntentActivities(rateAppIntent, 0).size() > 0)
    	    startActivity(rateAppIntent);
    	else
    		Toast.makeText(this, "Unable to open app in Google Play!", Toast.LENGTH_LONG).show();
    }
    
    private void shareApp() {
    	Intent shareIntent=new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT,"Ultimate PDF Converter v2");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "I recommend UPDFv2 for converting any file to pdf: \n"+
        										   "https://play.google.com/store/apps/details?id="+getPackageName());
        startActivity(Intent.createChooser(shareIntent, "Share..."));
    }

    private void selectItem(int position) {
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);
        if(position == SCREENS.PICK_FILE_TO_CONVERT.ordinal()) {
        	this.startFilePicker();
        	return;
        }
        if(position == SCREENS.RATE_APP.ordinal()) {
        	rateApp();
        	return;
        }
        if(position == SCREENS.SHARE_APP.ordinal()) {
        	shareApp();
        	return;
        }

        // Call respective screen handler
        if(screens.length > position && screens[position] != null)
        	screens[position].activate();
    }
    
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }	

	public void startFilePicker() {
		Intent filePicker = new Intent(this, NeoFilePicker.class);		    	
    	startActivityForResult(filePicker, FILE_PICKER_REQUEST);
	}

	public void startDirectoryPicker(DIRPickCB callback) {
		dirPickCallback = callback;
		Intent filePicker = new Intent(this, NeoFilePicker.class);
		filePicker.putExtra("DirectoryPick", true);
    	startActivityForResult(filePicker, DIR_PICKER_REQUEST);
	}
	
	public void startQueuedFiles() {
		screens[SCREENS.QUEUED_FILES.ordinal()].activate();
	}
	
	private void queueFileAndShow(File file) {
		if(file != null && file.exists() && file.isFile()) {
			Pair<String, String> filename = Util.splitFilename(file.getName());
			queuedb.queueFile(filename.first, filename.second, file.getParent());
			// Show Ad
			displayInterstitial();
			// Show Queued Files screen
			startQueuedFiles();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data ) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == FILE_PICKER_REQUEST) {
			if(resultCode == RESULT_OK) {
				if(data != null) {
					ArrayList<String> selectedFiles  = data.getStringArrayListExtra("FileList");
					if(selectedFiles != null && selectedFiles.size() > 0) {
						queueFileAndShow(new File(selectedFiles.get(0)));
						return;
					}
				}
			}
		} else if(requestCode == DIR_PICKER_REQUEST) {
			if(resultCode == RESULT_OK && data != null && dirPickCallback != null) {
				ArrayList<String> selectedDir = data.getStringArrayListExtra("FileList");
				if(selectedDir != null && selectedDir.size() > 0)
					dirPickCallback.dirPickCallback(selectedDir.get(0));
			}
		}
	}
}
