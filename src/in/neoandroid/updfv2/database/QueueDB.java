package in.neoandroid.updfv2.database;

import java.util.ArrayList;
import java.util.Collections;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class QueueDB extends SQLiteOpenHelper {
	Context dbContext;
	private static final String DATABASE_NAME = "queue.sql";
	private static final String TABLE = "UPDF_QUEUE";

	private static final String KEY_FILENAME = "filename";
	private static final String KEY_EXTENSION = "ext";
	private static final String KEY_PATH = "path";
	private static final String KEY_SAVED_AS = "saved_as";
	private static final String KEY_STATUS = "status";	// 0: Queued, 1: Converted, 2: Error
	private static final String KEY_SERVER_STATUS = "server_status";

	private static final int DB_VERSION = 1;
	private SQLiteDatabase sdb = null;
	private static QueueDB instance = null;

	public static class FileDetails extends Object {
		public static enum STATUS { QUEUED, CONVERTED, ERROR };
        public int id;
        public String filename;
        public String ext;
        public String path;
        public String saved_as;
        public int status;
        public String server_status;
        public String server_fileId; // Used when the conversion is in progress
    }

    private QueueDB(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
        dbContext = context;
        try {
        	if (sdb == null)
        		sdb = getWritableDatabase();
        } catch(Exception e) { e.printStackTrace(); }
    }

    public static QueueDB getInstance(Context context) {
    	if(instance == null)
    		instance = new QueueDB(context);
    	return instance;
    }

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE + "(" +
							   "_id integer primary key autoincrement, "+
							   KEY_FILENAME + " TEXT NOT NULL, " +
							   KEY_EXTENSION + " TEXT NOT NULL, " +
							   KEY_PATH + " TEXT DEFAULT NULL, " +
							   KEY_SAVED_AS + " TEXT DEFAULT NULL, " +
							   KEY_STATUS + " integer DEFAULT 0, " +
							   KEY_SERVER_STATUS + " TEXT DEFAULT NULL " +
							   ")";
		db.execSQL(CREATE_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		
	}
	
	public long queueFile(String filename, String extension, String path) {
		if(sdb == null)
			return 0;
		ContentValues cv = new ContentValues();
		cv.put(KEY_FILENAME, filename);
		cv.put(KEY_EXTENSION, extension);
		cv.put(KEY_PATH, path);
    	return sdb.insert(TABLE , null, cv);
	}

	public void deleteFile(int id) {
		if(sdb != null)
			sdb.delete(TABLE, "_id = ?", new String[] { String.valueOf(id) });
	}

	public ArrayList<FileDetails> getQueuedFiles() {
        return getFilesWithStatus(FileDetails.STATUS.QUEUED.ordinal(), false, null);
    }

	public ArrayList<FileDetails> getConvertedFiles() {
		ArrayList<FileDetails> files = getFilesWithStatus(FileDetails.STATUS.QUEUED.ordinal(), true, null);
		Collections.reverse(files);
		return files;
    }
	
	public FileDetails getFirstFile() {
		ArrayList<FileDetails> details = getFilesWithStatus(FileDetails.STATUS.QUEUED.ordinal(), false, "1");
		if(details != null && details.size() > 0)
			return details.get(0);
		return null;
	}

	public int updateStatus(FileDetails details) {
		if(sdb == null)
			return 0;
		ContentValues cv = new ContentValues();
		cv.put(KEY_SAVED_AS, details.saved_as);
		cv.put(KEY_STATUS, details.status);
		cv.put(KEY_SERVER_STATUS, details.server_status);
		return sdb.update(TABLE, cv, "_id = ?", new String[] { String.valueOf(details.id) });
	}

	private ArrayList<FileDetails> getFilesWithStatus(int status, boolean bNotEqual, String limit) {
		ArrayList<FileDetails> ret = new ArrayList<FileDetails>();
		if(sdb == null)
			return ret;
		String selection = KEY_STATUS + "= ?";
		if(bNotEqual)
			selection = KEY_STATUS + "<> ?";
		Cursor c = sdb.query(TABLE, null, selection, new String[] { String.valueOf(status) }, null, null, "_id", limit);
		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
            FileDetails details = new FileDetails();
            details.id = c.getInt(0);
            details.filename = c.getString(1);
            details.ext = c.getString(2);
            details.path = c.getString(3);
            details.saved_as = c.getString(4);
            details.status = c.getInt(5);
            details.server_status = c.getString(6);
            ret.add(details);
        }
		c.close();
		return ret;
	}
}
