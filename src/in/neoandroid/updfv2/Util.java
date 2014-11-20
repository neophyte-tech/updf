package in.neoandroid.updfv2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Pair;

public class Util {
	public static final String SHARED_PREFS_NAME = "UPDF";
	private static final String PREFS_SAME_DIR = "sameDirectory";
	private static final String PREFS_DEFAULT_DIR = "defaultDirectory";
	private static final String DEVICE_ID = "deviceID";

	private static SharedPreferences getSharedPreferences(Context context) {
		return context.getSharedPreferences(SHARED_PREFS_NAME, 0);
	}

	private static SharedPreferences.Editor getSharedPrefEditor(Context context) {
		return getSharedPreferences(context).edit();
	}

	public static Pair<String, String> splitFilename(String fullname) {
		String filename = fullname;
		String ext = "";
		int pos = fullname.lastIndexOf(".");
		if(pos >= 0) {
			ext = fullname.substring(pos+1);
			filename = (pos > 0) ? fullname.substring(0, pos) : "";
		}
        return new Pair<String, String>(filename, ext);
	}

	public static String joinFilename(String path, String filename, String ext) {
		String fullname = path;
		if(fullname.length() > 0 && fullname.charAt(fullname.length()-1) != '/')
			fullname = fullname + "/";
		fullname = fullname + filename;
		if(ext.length() > 0)
			fullname = fullname + "." + ext;
		return fullname;
	}

	public static boolean prefGetSameDirectorySave(Context context) {
		SharedPreferences settings = getSharedPreferences(context);
	    return settings.getBoolean(PREFS_SAME_DIR, false);
	}

	public static void prefSetSameDirectorySave(Context context, boolean bSet) {
		SharedPreferences.Editor editor = getSharedPrefEditor(context);
		editor.putBoolean(PREFS_SAME_DIR, bSet);
		editor.commit();
	}

	public static void prefSetDefaultDirectory(Context context, String dir) {
		SharedPreferences.Editor editor = getSharedPrefEditor(context);
		editor.putString(PREFS_DEFAULT_DIR, dir);
		editor.commit();
	}

	public static String prefGetDefaultDirectory(Context context) {
		SharedPreferences settings = getSharedPreferences(context);
	    return settings.getString(PREFS_DEFAULT_DIR, Environment.getExternalStorageDirectory().getAbsolutePath());
	}
	
	public static String prefGetDeviceID(Context context) {
		SharedPreferences settings = getSharedPreferences(context);
	    return settings.getString(DEVICE_ID, "");
	}
	
	public static void prefSetDeviceID(Context context, String deviceId) {
		SharedPreferences.Editor editor = getSharedPrefEditor(context);
		editor.putString(DEVICE_ID, deviceId);
		editor.commit();
	}
}