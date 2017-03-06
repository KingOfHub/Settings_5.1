package com.android.settings;

import com.android.settings.ethernet.EthernetConfigDialog;
import com.android.settings.ethernet.EthernetEnabler;

import android.app.ActionBar;
import android.app.Activity;
//import android.app.DockerManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.widget.Switch;

public class DockerSettings extends SettingsPreferenceFragment {

	private static final String KEY_USBHOST_TOGGLE = "host_toggle";

	private static final String KEY_ETH_TOGGLE = "eth_toggle";
	private static final String KEY_ETH_CONFIG = "eth_config";
	private static final String KEY_ETH_PROXY = "eth_proxy";
	
	private IntentFilter intentFilter;

	CheckBoxPreference hostToggle;
	
	CheckBoxPreference ethToggle;
	Preference ethConfig;
	PreferenceScreen ethProxy;
	
    private EthernetEnabler mEthEnabler;
    private EthernetConfigDialog mEthConfigDialog;
    
	@Override
	public void onCreate(Bundle icicle) {
		// TODO Auto-generated method stub
		super.onCreate(icicle);
		addPreferencesFromResource(R.xml.docker_settings);
		hostToggle = (CheckBoxPreference) findPreference(KEY_USBHOST_TOGGLE);
		ethToggle = (CheckBoxPreference) findPreference(KEY_ETH_TOGGLE);
		ethConfig = findPreference(KEY_ETH_CONFIG);
		ethProxy = (PreferenceScreen) findPreference(KEY_ETH_PROXY);
		intentFilter = new IntentFilter("docker.setting.change");
		final Activity activity = getActivity();
		initToggles();
	}
	
	private void initToggles() {
		hostToggle.setChecked(DockManager.getInstance().isEnable());
        mEthEnabler = new EthernetEnabler((CheckBoxPreference) findPreference(KEY_ETH_TOGGLE), getActivity());
        mEthConfigDialog = new EthernetConfigDialog(getActivity(), mEthEnabler, ethConfig);
        mEthEnabler.setConfigDialog(mEthConfigDialog);
    }
	
	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
			mEthEnabler.resume();
			getActivity().registerReceiver(mReceiver, intentFilter);
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
			mEthEnabler.pause();
			getActivity().unregisterReceiver(mReceiver);
	}
	
	public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        if (preference == ethConfig) {
            mEthConfigDialog.show();
        } else if(preference == ethToggle) {
        	syscWedigetStat();
        } else if(preference == hostToggle) {
			DockManager.getInstance().setEnabled(hostToggle.isChecked());
		}
        return false;
	}
	
	public void syscWedigetStat(){
		    hostToggle.setChecked(DockManager.getInstance().isEnable());
			ethConfig.setEnabled(ethToggle.isChecked());
			ethProxy.setEnabled(ethToggle.isChecked());
	}
	
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			syscWedigetStat();
		}
	};




}
