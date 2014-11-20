package in.neoandroid.updfv2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ScrollView;
import in.neoandroid.updfv2.UPDFActivity.UPDFScreen;

public class HomeScreen implements UPDFScreen, OnClickListener {
	private ScrollView svMainContent;
	private UPDFActivity parent;
	
	public HomeScreen(UPDFActivity parent, ScrollView svMainContent) {
		this.parent = parent;
		this.svMainContent = svMainContent;
	}

	@Override
	public void activate() {
		LayoutInflater Inflater = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		svMainContent.removeAllViews();
        View vHome = Inflater.inflate(R.layout.home, svMainContent);
        vHome.findViewById(R.id.bConvFileToPdf).setOnClickListener(this);
        parent.currentScreen = this;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		    case R.id.bConvFileToPdf:
		    	parent.startFilePicker();
	        	break;
		}
	}
}
