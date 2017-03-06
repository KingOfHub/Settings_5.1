package com.android.settings;
         
import com.android.settings.R;
import android.os.Bundle;
         
public class NetworkSettings extends  SettingsPreferenceFragment{
     	@Override
     	public void onCreate(Bundle icicle) {
         		super.onCreate(icicle);
                          addPreferencesFromResource(R.xml.network_settings);
        }
     
}