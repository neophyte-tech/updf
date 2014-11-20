package in.neoandroid.updfv2;

import java.io.File;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import in.neoandroid.updfv2.UPDFActivity.UPDFScreen;
import in.neoandroid.updfv2.database.QueueDB.FileDetails;

public class QueuedFilesScreen implements UPDFScreen, OnClickListener, OnLongClickListener {
	private ScrollView svMainContent;
	private UPDFActivity parent;
	private boolean bConvertedScreen = false;
	private TableLayout tlMain = null;
	private Thread refreshThread = new Thread() {
		@Override
		public void run() {
			while(true) {
				try {
					sleep(5000);
				} catch(Exception e) { }
				if(parent.currentScreen == QueuedFilesScreen.this) {
					parent.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							checkAndUpdateView();
						} 
					});
				}
			}
		}
	};
	
	private void checkAndUpdateView() {
		FileDetails firstFile = parent.queuedb.getFirstFile();
		FileDetails details = null;
		TableRow row = (TableRow)tlMain.getChildAt(0);
		if(row != null && row.getTag() != null && row.getTag() instanceof FileDetails) {
			details = (FileDetails) row.getTag();
			if(firstFile != null && firstFile.server_status != null)
				setFileStatus(row, firstFile.server_status);
		}

		if(details != firstFile && (firstFile == null || details == null || details.id != firstFile.id))
			activate();
	}
	
	public QueuedFilesScreen(UPDFActivity parent, ScrollView svMainContent, boolean isConvertedScreen) {
		this.parent = parent;
		this.svMainContent = svMainContent;
		this.bConvertedScreen = isConvertedScreen;
		if(!isConvertedScreen)
			refreshThread.start();
	}
	
	private void setFileStatus(TableRow row, String status) {
		TextView tvServerInfo = (TextView)row.findViewById(R.id.tvServerInfo);
		tvServerInfo.setText(status);
		tvServerInfo.setVisibility(View.VISIBLE);
	}

	@Override
	public void activate() {
		LayoutInflater inflater = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		svMainContent.removeAllViews();
        View vQueueFiles = inflater.inflate(R.layout.table_screen, svMainContent);
        tlMain = (TableLayout)svMainContent.findViewById(R.id.tlQueuedFiles);
        Button bButton = (Button)vQueueFiles.findViewById(R.id.bGenericButton);
        bButton.setText(bConvertedScreen ? R.string.view_queued_files : R.string.conv_more_files);
        bButton.setOnClickListener(this);
        ArrayList<FileDetails> queuedFiles = bConvertedScreen ? parent.queuedb.getConvertedFiles() : parent.queuedb.getQueuedFiles();

        if(queuedFiles != null && queuedFiles.size() > 0) {
        	tlMain.removeAllViews();
        	boolean bFirst = true;
        	for(FileDetails file : queuedFiles) {
        		TableRow row = (TableRow)inflater.inflate(R.layout.file, null);
        		((TextView)row.findViewById(R.id.tvFileName)).setText(Util.joinFilename("", file.filename, file.ext));
        		row.setTag(file);
        		row.setLongClickable(true);
        		row.setOnLongClickListener(this);
        		row.setOnClickListener(this);
        		if(bConvertedScreen) {
        			if(file.status != FileDetails.STATUS.CONVERTED.ordinal()) {
	        			TextView tvFileName = (TextView)row.findViewById(R.id.tvFileName);
	        			tvFileName.setTextColor(Color.RED);
        			}
        		} else {
        			if(file.server_status != null && file.server_status.length() > 0)
        				setFileStatus(row, file.server_status);
        			if(bFirst)
        				((ProgressBar)row.findViewById(R.id.progressBar)).setIndeterminate(true);
        		}
        		tlMain.addView(row);
        	}
        } else {
        	TextView tvFileName = ((TextView)tlMain.findViewById(R.id.tvFileName));
        	tvFileName.setText(bConvertedScreen ? R.string.conv_empty : R.string.queue_empty);
        	tvFileName.setLines(2);
        	tvFileName.setTypeface(Typeface.DEFAULT_BOLD);
        }
        parent.currentScreen = this;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		    case R.id.bGenericButton:
		    	if(bConvertedScreen)
		    		parent.startQueuedFiles();
		    	else
		    		parent.startFilePicker();
	        	break;
		    case R.id.trFileRow:
		    	if(bConvertedScreen) {
		    		if(v.getTag() != null && v.getTag() instanceof FileDetails) {
		    			// Open the saved PDF file
		    			FileDetails fileDetails = (FileDetails) v.getTag();
		    			if(fileDetails.status == FileDetails.STATUS.CONVERTED.ordinal()) {
			    			File file = new File(fileDetails.saved_as);
			    			if(!file.exists()) {
			    				Toast.makeText(parent, parent.getResources().getString(R.string.error_conv_filenotfound), Toast.LENGTH_LONG).show();
			    				return;
			    			}
			    			Intent intent = new Intent(Intent.ACTION_VIEW);
			    			intent.setDataAndType(Uri.fromFile(file), "application/pdf");
			    			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			    			try {
			    				parent.startActivity(intent);
			    			} catch(Exception e) {
			    				Toast.makeText(parent, parent.getResources().getString(R.string.error_no_viewer), Toast.LENGTH_LONG).show();
			    			}
		    			} else {
		    				new AlertDialog.Builder(parent)
			    				.setTitle(R.string.conv_error)
			    				.setMessage(fileDetails.server_status != null ? fileDetails.server_status : "Unknown Error")
			    				.setIcon(android.R.drawable.ic_dialog_alert)
			    				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			    				    public void onClick(DialogInterface dialog, int whichButton) {
			    				    	return;
			    				    }}).show();
		    			}
		    		}
		    	}
		    	break;
		}
	}

	@Override
	public boolean onLongClick(View v) {
		if(v != null && v instanceof TableRow) {
			final TableRow row = (TableRow) v;
			new AlertDialog.Builder(parent)
				.setTitle(R.string.confirmation)
				.setMessage(bConvertedScreen ? R.string.sure_conv : R.string.sure_queue)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int whichButton) {
				    	FileDetails fileDetails = (FileDetails)row.getTag();
				        row.setVisibility(View.GONE);
				    	tlMain.removeView(row);
				    	parent.queuedb.deleteFile(fileDetails.id);
				    }})
				 .setNegativeButton(android.R.string.no, null).show();
		}
		return false;
	}
}
