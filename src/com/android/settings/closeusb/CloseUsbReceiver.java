package com.android.settings.closeusb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.device.UfsManager;
import android.util.Log;
import android.provider.Settings;

import org.json.JSONException;
import org.json.JSONObject;
public class CloseUsbReceiver extends BroadcastReceiver {
    private boolean mUsb = true;
    private static final String LOG_TAG = "CloseUsbReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		UfsManager manager = new UfsManager();
        if(manager.init() == 0) {
            byte[] config = new byte[4096];
            int ret = manager.getConfig(config);
            if(ret > 0 &&  ret < config.length )
            parseJSONString(new String(config, 0 , ret));
            manager.release();
        }     
        if(!mUsb){
            Settings.Global.putInt(context.getContentResolver(),Settings.Global.ADB_ENABLED, 0);
        }
        
	}
	private void parseJSONString(String JSONString) {
        try {
            JSONObject config = new JSONObject(JSONString);
            JSONObject ShowdownUI = config.getJSONObject("SettingsUI");
            if(ShowdownUI != null) {
            	mUsb = (ShowdownUI.getInt("usb_debug") == 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mUsb = true;
        }
        android.util.Log.i(LOG_TAG,"parseJSONString mUsb=" + mUsb);
    }
}
