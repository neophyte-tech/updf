package in.neoandroid.updfv2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import in.neoandroid.updfv2.UPDFActivity.UPDFScreen;
import in.neoandroid.updfv2.UPDFActivity.DIRPickCB;

public class SettingsScreen implements UPDFScreen, OnClickListener, DIRPickCB {
	private ScrollView svMainContent;
	private UPDFActivity parent;
	private CheckBox cbSameDir;
	private EditText etFallback;
	
	public SettingsScreen(UPDFActivity parent, ScrollView svMainContent) {
		this.parent = parent;
		this.svMainContent = svMainContent;
	}

	public void saveSettings() {
		Util.prefSetSameDirectorySave(parent, cbSameDir.isChecked());
		Util.prefSetDefaultDirectory(parent, etFallback.getText().toString());
		Toast.makeText(parent, "Settings Saved...", Toast.LENGTH_LONG).show();
	}

	@Override
	public void activate() {
		LayoutInflater Inflater = (LayoutInflater) parent.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		svMainContent.removeAllViews();
        View vSettings = Inflater.inflate(R.layout.settings, svMainContent);
        vSettings.findViewById(R.id.bSaveSettings).setOnClickListener(this);
        vSettings.findViewById(R.id.tvSameDir).setOnClickListener(this);
        
        vSettings.findViewById(R.id.etFallback).setOnClickListener(this);
        vSettings.findViewById(R.id.bPick).setOnClickListener(this);
        cbSameDir = (CheckBox)vSettings.findViewById(R.id.cbSameDir);
        etFallback = (EditText)vSettings.findViewById(R.id.etFallback);
        cbSameDir.setChecked(Util.prefGetSameDirectorySave(parent));
        etFallback.setText(Util.prefGetDefaultDirectory(parent));
        etFallback.setSelection(etFallback.getText().length());
        parent.currentScreen = this;
	}

	@Override
	public void dirPickCallback(String dir) {
		if(etFallback != null && dir != null) {
			etFallback.setText(dir);
			etFallback.setSelection(etFallback.getText().length());
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bSaveSettings:
				saveSettings();
				break;

			case R.id.tvSameDir:
				cbSameDir.setChecked(!cbSameDir.isChecked());
				break;

			case R.id.etFallback:
			case R.id.bPick:
				parent.startDirectoryPicker(this);
				break;
		}
	}
}
