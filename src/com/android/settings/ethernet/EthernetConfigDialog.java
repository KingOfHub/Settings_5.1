/* Copyright (C) 2010 The Android-x86 Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Yi Sun <beyounn@gmail.com>
 */

package com.android.settings.ethernet;


import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.settings.R;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.IpConfiguration.ProxySettings;
import android.net.LinkAddress;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkUtils;
import android.net.StaticIpConfiguration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Slog;

public class EthernetConfigDialog extends AlertDialog implements android.view.View.OnClickListener  {
    private final String TAG = "EthernetSettings";
    private static final boolean localLOGV = true;

    private EthernetEnabler mEthEnabler;
    private View mView;
    private RadioButton mConTypeDhcp;
    private RadioButton mConTypeManual;
    private EditText mIpaddr;
    private EditText mDns1;
    private EditText mGw;
    private EditText mMask;
    private EditText mDns2;
    private Button mSaveButton;
    private Button mCancelButton;
    private IpConfiguration mConfiguration;
    


    private EthernetManager mEthManager;
    private boolean mEnablePending;
    private ConnectivityManager connManager;
    private Preference mRelationPref;

    public EthernetConfigDialog(Context context, EthernetEnabler Enabler, Preference relationPref) {
        super(context);
        mEthEnabler = Enabler;
         mEthManager = (EthernetManager)context.getSystemService(Context.ETHERNET_SERVICE);
        connManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connManager.getActiveNetworkInfo();
        buildDialogContent(context);
        mRelationPref = relationPref;
    }

    public int buildDialogContent(Context context) {
    	NetworkInfo info = connManager.getActiveNetworkInfo();
    	boolean ethConnect = info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_ETHERNET;
        this.setTitle(R.string.eth_config_title);
        this.setView(mView = getLayoutInflater().inflate(R.layout.eth_configure, null));
        mConTypeDhcp = (RadioButton) mView.findViewById(R.id.dhcp_radio);
        mConTypeManual = (RadioButton) mView.findViewById(R.id.manual_radio);
        mIpaddr = (EditText)mView.findViewById(R.id.ipaddr_edit);
        mMask = (EditText)mView.findViewById(R.id.netmask_edit);
        mDns1 = (EditText)mView.findViewById(R.id.eth_dns_edit1);
        mDns2 = (EditText)mView.findViewById(R.id.eth_dns_edit2);
        mGw = (EditText)mView.findViewById(R.id.eth_gw_edit);
        mSaveButton = (Button)mView.findViewById(R.id.save);
        mCancelButton = (Button)mView.findViewById(R.id.cancel);
        mSaveButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);


        mConTypeDhcp.setChecked(true);
        mConTypeManual.setChecked(false);
        mIpaddr.setEnabled(false);
        mMask.setEnabled(false);
        mDns1.setEnabled(false);
        mDns2.setEnabled(false);
        mGw.setEnabled(false);
        mConTypeManual.setOnClickListener(new RadioButton.OnClickListener() {
            public void onClick(View v) {
                mIpaddr.setEnabled(true);
                mDns1.setEnabled(true);
                mDns2.setEnabled(true);
                mGw.setEnabled(true);
                mMask.setEnabled(true);
            }
        });

        mConTypeDhcp.setOnClickListener(new RadioButton.OnClickListener() {
           public void onClick(View v) {
                mIpaddr.setEnabled(false);
                mDns1.setEnabled(false);
                mDns2.setEnabled(false);
                mGw.setEnabled(false);
                mMask.setEnabled(false);
            }
        });

        this.setInverseBackgroundForced(true);
        return 0;
    }
    
    @Override
	public void show() {
		// TODO Auto-generated method stub
    	mConfiguration = mEthManager.getConfiguration();
		if (mConfiguration != null) {
			StaticIpConfiguration staticIpconfig = mConfiguration.getStaticIpConfiguration();
			if (staticIpconfig != null) {
				String ipAdd = staticIpconfig.ipAddress != null ? (staticIpconfig.ipAddress.getAddress() != null
						? staticIpconfig.ipAddress.getAddress().getHostAddress() : null) : null;
				String gwADD = staticIpconfig.gateway != null ? staticIpconfig.gateway.getHostAddress() : null;
				String dns1 = null;
				String dns2 = null;
				if(staticIpconfig.dnsServers !=null && staticIpconfig.dnsServers.size() > 0){
					dns1 = staticIpconfig.dnsServers.get(0) != null?staticIpconfig.dnsServers.get(0).getHostAddress():null;
				}
				if(staticIpconfig.dnsServers !=null && staticIpconfig.dnsServers.size() > 1){
					dns2 = staticIpconfig.dnsServers.get(1) != null?staticIpconfig.dnsServers.get(1).getHostAddress():null;
				}
				String mask = staticIpconfig.ipAddress != null ? Integer.toString(staticIpconfig.ipAddress.getNetworkPrefixLength()) : null;
				mIpaddr.setText(ipAdd);
				mGw.setText(gwADD);
				mDns1.setText(dns1);
				mDns2.setText(dns2);
				mMask.setText(mask);
			}	
				boolean isDHCP = IpConfiguration.IpAssignment.DHCP.equals(mConfiguration.ipAssignment);
				mConTypeDhcp.setChecked(isDHCP);
				mConTypeManual.setChecked(!isDHCP);
				mIpaddr.setEnabled(!isDHCP);
				mDns1.setEnabled(!isDHCP);
				mDns2.setEnabled(!isDHCP);
				mGw.setEnabled(!isDHCP);
				mMask.setEnabled(!isDHCP);
			
		}
		super.show();
	}
    
    private void setConfigEnable(boolean enabled){
    	mRelationPref.setEnabled(enabled);
    	mRelationPref.setSummary(enabled? R.string.eth_conf_summary :  R.string.configuring_ethernet);
    }

    private int handle_saveconf() {
    	if(mConfiguration == null){
    		mConfiguration = new IpConfiguration(IpAssignment.DHCP, ProxySettings.NONE, null, null);
    	}
        if (mConTypeDhcp.isChecked()) {
        	mConfiguration.ipAssignment = IpAssignment.DHCP;
        	mConfiguration.proxySettings = ProxySettings.NONE;
        } else {
            Slog.v(TAG, "Config device for static " + mIpaddr.getText().toString() + mGw.getText().toString() + mDns1.getText().toString() + mMask.getText().toString());
            mConfiguration.ipAssignment = IpAssignment.STATIC;
            String ipAdd = mIpaddr.getText().toString();
            String gwAdd = mGw.getText().toString();
            String dns1 = mDns1.getText().toString();
            String dns2 = mDns2.getText().toString();
            String mask = mMask.getText().toString();
            Inet4Address inetAddr = null;
            Inet4Address gwAddr = null;
            Inet4Address dnsAddr1 = null;
            Inet4Address dnsAddr2 = null;
            int intMask = -1;
            try {
            	inetAddr = (Inet4Address) NetworkUtils.numericToInetAddress(ipAdd);
            } catch (IllegalArgumentException|ClassCastException e) {
            	return R.string.wifi_ip_settings_invalid_ip_address;
            }
            try {
            	gwAddr = (Inet4Address) NetworkUtils.numericToInetAddress(gwAdd);
            } catch (IllegalArgumentException|ClassCastException e) {
            	return R.string.wifi_ip_settings_invalid_gateway;
            }
            try {
            	dnsAddr1 = (Inet4Address) NetworkUtils.numericToInetAddress(dns1);
            } catch (IllegalArgumentException|ClassCastException e) {
            	return R.string.wifi_ip_settings_invalid_dns;
            }
            try {
            	dnsAddr2 = (Inet4Address) NetworkUtils.numericToInetAddress(dns2);
            } catch (IllegalArgumentException|ClassCastException e) {
            	return R.string.wifi_ip_settings_invalid_dns;
            }
            try {
            	intMask = Integer.parseInt(mask);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            if(intMask < 0 || intMask > 32){
            	 return R.string.wifi_ip_settings_invalid_network_prefix_length;
            }
            StaticIpConfiguration staticIpConfiguration = new StaticIpConfiguration();
            
            staticIpConfiguration.ipAddress = new LinkAddress(inetAddr,intMask);
            staticIpConfiguration.gateway = gwAddr;
            staticIpConfiguration.dnsServers.add(dnsAddr1);
            staticIpConfiguration.dnsServers.add(dnsAddr2);
            mConfiguration.setStaticIpConfiguration(staticIpConfiguration);
            
            
        }
        new SaveConfigTask().execute();
        return 0;
    }

	@Override
	public void onClick(View view) {
		// TODO Auto-generated method stub
		switch(view.getId()){
		case R.id.save:
			int result = handle_saveconf();
			if(result == 0){
				this.dismiss();
			}else{
				showErrorToast(getContext(),result);
			}
			break;
		case R.id.cancel:
			this.dismiss();
			break;
		}
		
	}
	class SaveConfigTask extends AsyncTask<Void, Void, Void>{

		@Override
		protected Void doInBackground(Void... arg0) {
			// TODO Auto-generated method stub
	        mEthManager.setConfiguration(mConfiguration);
			return null;
		}
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			setConfigEnable(false);

		}
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			setConfigEnable(true);
		}




	}
	private void showErrorToast(Context context,int id){
		Toast.makeText(context, id, Toast.LENGTH_LONG).show();
	}
    
    
}

