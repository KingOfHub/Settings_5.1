/*
 * Copyright (C) 2008 The Android Open Source Project
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
 */

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.SELinux;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.device.UfsManager;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Index;
import com.android.settings.search.Indexable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.device.MaxqManager;

public class DeviceInfoSettings extends SettingsPreferenceFragment implements Indexable,DialogInterface.OnClickListener {

    private static final String LOG_TAG = "DeviceInfoSettings";
    private static final String FILENAME_PROC_VERSION = "/proc/version";
    private static final String FILENAME_MSV = "/sys/board_properties/soc/msv";
    private static final String OTA_UPDATE_SYSTEM = "android.settings.SYSTEM_UPDATE";
    private static final String FILENAME_PROC_CPUINFO = "/proc/cpuinfo";

    private static final String KEY_CONTAINER = "container";
    private static final String KEY_REGULATORY_INFO = "regulatory_info";
    private static final String KEY_TERMS = "terms";
    private static final String KEY_LICENSE = "license";
    private static final String KEY_COPYRIGHT = "copyright";
    private static final String KEY_WEBVIEW_LICENSE = "webview_license";
    private static final String KEY_SYSTEM_UPDATE_SETTINGS = "system_update_settings";
    private static final String PROPERTY_URL_SAFETYLEGAL = "ro.url.safetylegal";
    private static final String PROPERTY_SELINUX_STATUS = "ro.build.selinux";
    private static final String KEY_KERNEL_VERSION = "kernel_version";
    private static final String KEY_BUILD_NUMBER = "build_number";
    private static final String KEY_DEVICE_MODEL = "device_model";
    private static final String KEY_DEVICE_PROCESSOR = "device_processor";
    private static final String KEY_SELINUX_STATUS = "selinux_status";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    private static final String KEY_FIRMWARE_VERSION = "firmware_version";
    private static final String KEY_CUSTOM_BUILD_VERSION = "custom_build_version";
    private static final String KEY_SECURITY_PATCH = "security_patch";
    private static final String KEY_UPDATE_SETTING = "additional_system_update_settings";
    private static final String KEY_EQUIPMENT_ID = "fcc_equipment_id";
    private static final String PROPERTY_EQUIPMENT_ID = "ro.ril.fccid";
    private static final String KEY_DEVICE_FEEDBACK = "device_feedback";
    private static final String KEY_DEVELOP_LOGIN = "develop_login";
    private static final String KEY_SAFETY_LEGAL = "safetylegal";
    private static final String KEY_STATUS = "status_info";
    private static final String KEY_DEVICE_SN = "device_sn";
    private static final String KEY_32550 = "device_32550";
    private static final String KEY_UFS_VER = "device_ufs_version";
    static final int TAPS_TO_BE_A_DEVELOPER = 7;
    private static boolean mHideVersionName = false;

    private static final int DIALOG_LOGIN_SETTINGS = 1;
    private AdminLoginDialog mDialog;
    private static final String ADMIN_ACCOUNT = "admin";
    private static final String ADMIN_PASSWORD = "u123456";

    long[] mHits = new long[3];
    int mDevHitCountdown;
    Toast mDevHitToast;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mHideVersionName = getResources().getBoolean(R.bool.def_hide_kernel_version_name);

        addPreferencesFromResource(R.xml.device_info_settings);
        if (getResources().getBoolean(R.bool.def_device_info_ota_update_enable)) {
            if (getResources().getBoolean(R.bool.def_carrier_ota_enable)) {
                String address = getResources().getString(R.string.def_carrier_ota_address);
                if (!TextUtils.isEmpty(address)) {
                    findPreference(KEY_SYSTEM_UPDATE_SETTINGS).setIntent(
                            new Intent(address));
                }
            } else {
                findPreference(KEY_SYSTEM_UPDATE_SETTINGS).setIntent(
                        new Intent(OTA_UPDATE_SYSTEM));
            }
        }
        setStringSummary(KEY_FIRMWARE_VERSION, Build.VERSION.RELEASE);
        findPreference(KEY_FIRMWARE_VERSION).setEnabled(true);
        if(Build.PWV_CUSTOM_CUSTOM.equals("SYB")){
            findPreference(KEY_DEVELOP_LOGIN).setEnabled(true);
        }
        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            setStringSummary(KEY_SECURITY_PATCH, patch);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_SECURITY_PATCH));
        }
        setValueSummary(KEY_BASEBAND_VERSION, "gsm.version.baseband");
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL + getMsvSuffix());
        setStringSummary(KEY_DEVICE_PROCESSOR, getDeviceProcessorInfo());
        setValueSummary(KEY_EQUIPMENT_ID, PROPERTY_EQUIPMENT_ID);
        setStringSummary(KEY_DEVICE_MODEL, Build.MODEL);
        String sn = android.device.provider.Settings.System.getString(getContentResolver(),"device_sn");
        if(!TextUtils.isEmpty(sn)){
        	setStringSummary(KEY_DEVICE_SN, sn);
        }
        setStringSummary(KEY_BUILD_NUMBER, Build.DISPLAY);
        String customVersion = getResources().getString(R.string.def_build_version_defname);
        if (!TextUtils.isEmpty(customVersion)) {
            setStringSummary(KEY_CUSTOM_BUILD_VERSION, customVersion);
        } else {
            getPreferenceScreen().removePreference(findPreference(KEY_CUSTOM_BUILD_VERSION));
        }
        findPreference(KEY_BUILD_NUMBER).setEnabled(true);
        findPreference(KEY_KERNEL_VERSION).setSummary(getFormattedKernelVersion());

        if (!SELinux.isSELinuxEnabled()) {
            String status = getResources().getString(R.string.selinux_status_disabled);
            setStringSummary(KEY_SELINUX_STATUS, status);
        } else if (!SELinux.isSELinuxEnforced()) {
            String status = getResources().getString(R.string.selinux_status_permissive);
            setStringSummary(KEY_SELINUX_STATUS, status);
        }
        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
            findPreference(KEY_STATUS).getIntent().setClassName(
                    "com.android.settings", "com.android.settings.deviceinfo.MSimStatus");
        }
        if(android.os.SystemProperties.getBoolean("pwv.se.support", false)) {
        	String version = get32550Versino();
        	if(version != null && version.length() > 0)
        		findPreference(KEY_32550).setSummary(version);
        } else {
        	try {
                getPreferenceScreen().removePreference(findPreference(KEY_32550));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "' missing and no '"
                        + KEY_32550 + "' preference");
            }
        }

        if(Build.PWV_CUSTOM_CUSTOM.equals("SYB")){
            setStringSummary(KEY_DEVICE_MODEL,
                    "UROVO " + SystemProperties.get("persist.env.sys.modelName", Build.CUSTOM_MODEL));
            setStringSummary(KEY_DEVICE_PROCESSOR, getDeviceProcessorInfo());
        }else{
            removePreference(KEY_DEVICE_PROCESSOR);
        }

		//
		UfsManager manager = new UfsManager();
        if(manager.init() == 0) {
            byte[] usfModel = new byte[64];
            int ret = manager.getPkgVersion(usfModel);
            String uVerinfo = new String(usfModel, 0 , ret);
            findPreference(KEY_UFS_VER).setSummary(uVerinfo);
        } else {
            try {
                getPreferenceScreen().removePreference(findPreference(KEY_UFS_VER));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "' missing and no '"
                        + KEY_32550 + "' preference");
            }
        }
        // Remove selinux information if property is not present
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SELINUX_STATUS,
                PROPERTY_SELINUX_STATUS);

        // Remove Safety information preference if PROPERTY_URL_SAFETYLEGAL is not set
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_SAFETY_LEGAL,
                PROPERTY_URL_SAFETYLEGAL);

        // Remove Equipment id preference if FCC ID is not set by RIL
        removePreferenceIfPropertyMissing(getPreferenceScreen(), KEY_EQUIPMENT_ID,
                PROPERTY_EQUIPMENT_ID);

        // Remove Baseband version if wifi-only device
        if (Utils.isWifiOnly(getActivity())) {
            getPreferenceScreen().removePreference(findPreference(KEY_BASEBAND_VERSION));
        }

        // Dont show feedback option if there is no reporter.
        if (TextUtils.isEmpty(getFeedbackReporterPackage(getActivity()))) {
            getPreferenceScreen().removePreference(findPreference(KEY_DEVICE_FEEDBACK));
        }

        /*
         * Settings is a generic app and should not contain any device-specific
         * info.
         */
        final Activity act = getActivity();
        // These are contained in the "container" preference group
        PreferenceGroup parentPreference = (PreferenceGroup) findPreference(KEY_CONTAINER);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_TERMS,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_COPYRIGHT,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference, KEY_WEBVIEW_LICENSE,
                Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);

        // These are contained by the root preference screen
        parentPreference = getPreferenceScreen();
        if (UserHandle.myUserId() == UserHandle.USER_OWNER) {
            Utils.updatePreferenceToSpecificActivityOrRemove(act, parentPreference,
                    KEY_SYSTEM_UPDATE_SETTINGS,
                    Utils.UPDATE_PREFERENCE_FLAG_SET_TITLE_TO_MATCHING_ACTIVITY);
        } else {
            // Remove for secondary users
            removePreference(KEY_SYSTEM_UPDATE_SETTINGS);
        }

        // Read platform settings for additional system update setting
        removePreferenceIfBoolFalse(KEY_UPDATE_SETTING,
                R.bool.config_additional_system_update_setting_enable);

        // Remove regulatory information if none present.
        if (!getResources().getBoolean(R.bool.def_device_info_ota_update_enable) ||
                !getResources().getBoolean(R.bool.def_regulatory_enable)) {
            final Intent intent = new Intent(Settings.ACTION_SHOW_REGULATORY_INFO);
            if (getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
                Preference pref = findPreference(KEY_REGULATORY_INFO);
                if (pref != null) {
                    getPreferenceScreen().removePreference(pref);
                }
            }
        }

        // Remove KEY_REGULATORY_INFO by default, user can't select the regulatory info
        removePreference(KEY_REGULATORY_INFO);

        if(Build.PWV_CUSTOM_CUSTOM.equals("SYB")){
            removePreference(KEY_STATUS);
            removePreference(KEY_CONTAINER);
            removePreference(KEY_SELINUX_STATUS);
            removePreference(KEY_BASEBAND_VERSION);
            removePreference(KEY_KERNEL_VERSION);
            removePreference(KEY_32550);
            removePreference(KEY_SECURITY_PATCH);
            findPreference(KEY_DEVICE_MODEL).setOrder(1);
            findPreference(KEY_DEVICE_SN).setOrder(2);
            findPreference(KEY_FIRMWARE_VERSION).setOrder(3);
            findPreference(KEY_DEVICE_PROCESSOR).setOrder(4);
            findPreference(KEY_BUILD_NUMBER).setOrder(5);
            findPreference(KEY_SYSTEM_UPDATE_SETTINGS).setOrder(6);
            findPreference(KEY_DEVELOP_LOGIN).setOrder(7);
        }else{
            removePreference(KEY_DEVELOP_LOGIN);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mDevHitCountdown = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(DevelopmentSettings.PREF_SHOW,
                        android.os.Build.TYPE.equals("eng")) ? -1 : TAPS_TO_BE_A_DEVELOPER;

        mDevHitToast = null;
    }

    private boolean isHiddenDeveloper(){
        return getResources().getBoolean(R.bool.def_hidden_developer);
    }

    /**********add by dengtonglong(20.17.2.22)******************/
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_LOGIN_SETTINGS) {
            mDialog = new AdminLoginDialog(getActivity(), this);
            return mDialog;
        }
        return null;
    }
    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            CharSequence mUserName = mDialog.getUser();
            CharSequence mPassword = mDialog.getPassword();
            if (Build.PWV_CUSTOM_CUSTOM.equals("SYB")) {
                if (mUserName.toString()
                        .equals(ADMIN_ACCOUNT)
                        && mPassword.toString()
                        .equals(ADMIN_PASSWORD)) {
                    Toast.makeText(DeviceInfoSettings.this.getActivity(),
                            R.string.password_checkout_true, Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(DeviceInfoSettings.this.getActivity(),
                            R.string.password_checkout_false, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    /**********end******************/

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals(KEY_FIRMWARE_VERSION)) {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length-1);
            mHits[mHits.length-1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis()-500)) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("android",
                        com.android.internal.app.PlatLogoActivity.class.getName());
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Unable to start activity " + intent.toString());
                }
            }
        } else if (preference.getKey().equals(KEY_BUILD_NUMBER)) {
            // Don't enable developer options for secondary users.
            if (UserHandle.myUserId() != UserHandle.USER_OWNER) return true;

            final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            if (um.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES)) return true;

            if (mDevHitCountdown > 0) {
                mDevHitCountdown--;
                if (mDevHitCountdown == 0) {
                    getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                            Context.MODE_PRIVATE).edit().putBoolean(
                                    DevelopmentSettings.PREF_SHOW, true).apply();
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_on,
                            Toast.LENGTH_LONG);
                    mDevHitToast.show();
                    // This is good time to index the Developer Options
                    Index.getInstance(
                            getActivity().getApplicationContext()).updateFromClassNameResource(
                                    DevelopmentSettings.class.getName(), true, true);

                } else if (mDevHitCountdown > 0
                        && mDevHitCountdown < (TAPS_TO_BE_A_DEVELOPER-2)) {
                    if (mDevHitToast != null) {
                        mDevHitToast.cancel();
                    }
                    mDevHitToast = Toast.makeText(getActivity(), getResources().getQuantityString(
                            R.plurals.show_dev_countdown, mDevHitCountdown, mDevHitCountdown),
                            Toast.LENGTH_SHORT);
                    mDevHitToast.show();
                }
            } else if (mDevHitCountdown < 0) {
                if (mDevHitToast != null) {
                    mDevHitToast.cancel();
                }
                mDevHitToast = Toast.makeText(getActivity(), R.string.show_dev_already,
                        Toast.LENGTH_LONG);
                mDevHitToast.show();
            }
        } else if (preference.getKey().equals(KEY_DEVICE_FEEDBACK)) {
            sendFeedback();
        } else if (preference.getKey().equals(KEY_DEVELOP_LOGIN)){
            showDialog(DIALOG_LOGIN_SETTINGS);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void removePreferenceIfPropertyMissing(PreferenceGroup preferenceGroup,
            String preference, String property ) {
        if (SystemProperties.get(property).equals("")) {
            // Property is missing so remove preference from group
            try {
                preferenceGroup.removePreference(findPreference(preference));
            } catch (RuntimeException e) {
                Log.d(LOG_TAG, "Property '" + property + "' missing and no '"
                        + preference + "' preference");
            }
        }
    }

    private void removePreferenceIfBoolFalse(String preference, int resId) {
        if (!getResources().getBoolean(resId)) {
            Preference pref = findPreference(preference);
            if (pref != null) {
                getPreferenceScreen().removePreference(pref);
            }
        }
    }

    private void setStringSummary(String preference, String value) {
        try {
            findPreference(preference).setSummary(value);
        } catch (RuntimeException e) {
            findPreference(preference).setSummary(
                getResources().getString(R.string.device_info_default));
        }
    }

    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(
                    SystemProperties.get(property,
                            getResources().getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }

    private void sendFeedback() {
        String reporterPackage = getFeedbackReporterPackage(getActivity());
        if (TextUtils.isEmpty(reporterPackage)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(reporterPackage);
        startActivityForResult(intent, 0);
    }

    /**
     * Reads a line from the specified file.
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
        try {
            return reader.readLine();
        } finally {
            reader.close();
        }
    }

    public static String getFormattedKernelVersion() {
        try {
            return formatKernelVersion(readLine(FILENAME_PROC_VERSION));

        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting kernel version for Device Info screen",
                e);

            return "Unavailable";
        }
    }

    public static String formatKernelVersion(String rawKernelVersion) {
        // Example (see tests for more):
        // Linux version 3.0.31-g6fb96c9 (android-build@xxx.xxx.xxx.xxx.com) \
        //     (gcc version 4.6.x-xxx 20120106 (prerelease) (GCC) ) #1 SMP PREEMPT \
        //     Thu Jun 28 11:02:39 PDT 2012

        final String PROC_VERSION_REGEX =
            "Linux version (\\S+) " + /* group 1: "3.0.31-g6fb96c9" */
            "\\((\\S+?)\\) " +        /* group 2: "x@y.com" (kernel builder) */
            "(?:\\(gcc.+? \\)) " +    /* ignore: GCC version information */
            "(#\\d+) " +              /* group 3: "#1" */
            "(?:.*?)?" +              /* ignore: optional SMP, PREEMPT, and any CONFIG_FLAGS */
            "((Sun|Mon|Tue|Wed|Thu|Fri|Sat).+)"; /* group 4: "Thu Jun 28 11:02:39 PDT 2012" */

        Matcher m = Pattern.compile(PROC_VERSION_REGEX).matcher(rawKernelVersion);
        if (!m.matches()) {
            Log.e(LOG_TAG, "Regex did not match on /proc/version: " + rawKernelVersion);
            return "Unavailable";
        } else if (m.groupCount() < 4) {
            Log.e(LOG_TAG, "Regex match on /proc/version only returned " + m.groupCount()
                    + " groups");
            return "Unavailable";
        }

        if (mHideVersionName) {
            return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
                m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
        } else {
            return m.group(1) + "\n" +                 // 3.0.31-g6fb96c9
                m.group(2) + " " + m.group(3) + "\n" + // x@y.com #1
                m.group(4);                            // Thu Jun 28 11:02:39 PDT 2012
        }
    }

    /**
     * Returns " (ENGINEERING)" if the msv file has a zero value, else returns "".
     * @return a string to append to the model number description.
     */
    private String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine(FILENAME_MSV);
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException ioe) {
            // Fail quietly, as the file may not exist on some devices.
        } catch (NumberFormatException nfe) {
            // Fail quietly, returning empty string should be sufficient
        }
        return "";
    }

    private static String getFeedbackReporterPackage(Context context) {
        final String feedbackReporter =
                context.getResources().getString(R.string.oem_preferred_feedback_reporter);
        if (TextUtils.isEmpty(feedbackReporter)) {
            // Reporter not configured. Return.
            return feedbackReporter;
        }
        // Additional checks to ensure the reporter is on system image, and reporter is
        // configured to listen to the intent. Otherwise, dont show the "send feedback" option.
        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolvedPackages =
                pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : resolvedPackages) {
            if (info.activityInfo != null) {
                if (!TextUtils.isEmpty(info.activityInfo.packageName)) {
                    try {
                        ApplicationInfo ai = pm.getApplicationInfo(info.activityInfo.packageName, 0);
                        if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                            // Package is on the system image
                            if (TextUtils.equals(
                                        info.activityInfo.packageName, feedbackReporter)) {
                                return feedbackReporter;
                            }
                        }
                    } catch (PackageManager.NameNotFoundException e) {
                         // No need to do anything here.
                    }
                }
            }
        }
        return null;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {

            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(
                    Context context, boolean enabled) {
                final SearchIndexableResource sir = new SearchIndexableResource(context);
                sir.xmlResId = R.xml.device_info_settings;
                return Arrays.asList(sir);
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                final List<String> keys = new ArrayList<String>();
                if (isPropertyMissing(PROPERTY_SELINUX_STATUS)) {
                    keys.add(KEY_SELINUX_STATUS);
                }
                if (isPropertyMissing(PROPERTY_URL_SAFETYLEGAL)) {
                    keys.add(KEY_SAFETY_LEGAL);
                }
                if (isPropertyMissing(PROPERTY_EQUIPMENT_ID)) {
                    keys.add(KEY_EQUIPMENT_ID);
                }
                // Remove Baseband version if wifi-only device
                if (Utils.isWifiOnly(context)) {
                    keys.add((KEY_BASEBAND_VERSION));
                }
                // Dont show feedback option if there is no reporter.
                if (TextUtils.isEmpty(getFeedbackReporterPackage(context))) {
                    keys.add(KEY_DEVICE_FEEDBACK);
                }
                if (!checkIntentAction(context, "android.settings.TERMS")) {
                    keys.add(KEY_TERMS);
                }
                if (!checkIntentAction(context, "android.settings.LICENSE")) {
                    keys.add(KEY_LICENSE);
                }
                if (!checkIntentAction(context, "android.settings.COPYRIGHT")) {
                    keys.add(KEY_COPYRIGHT);
                }
                if (!checkIntentAction(context, "android.settings.WEBVIEW_LICENSE")) {
                    keys.add(KEY_WEBVIEW_LICENSE);
                }
                if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
                    keys.add(KEY_SYSTEM_UPDATE_SETTINGS);
                }
                if (!context.getResources().getBoolean(
                        R.bool.config_additional_system_update_setting_enable)) {
                    keys.add(KEY_UPDATE_SETTING);
                }
                return keys;
            }

            private boolean isPropertyMissing(String property) {
                return SystemProperties.get(property).equals("");
            }

            private boolean checkIntentAction(Context context, String action) {
                final Intent intent = new Intent(action);

                // Find the activity that is in the system image
                final PackageManager pm = context.getPackageManager();
                final List<ResolveInfo> list = pm.queryIntentActivities(intent, 0);
                final int listSize = list.size();

                for (int i = 0; i < listSize; i++) {
                    ResolveInfo resolveInfo = list.get(i);
                    if ((resolveInfo.activityInfo.applicationInfo.flags &
                            ApplicationInfo.FLAG_SYSTEM) != 0) {
                        return true;
                    }
                }

                return false;
            }
        };

    /**
     * Returns the Hardware value in /proc/cpuinfo, else returns "Unknown".
     * @return a string that describes the processor
     */
    private static String getDeviceProcessorInfo() {
        // Hardware : XYZ
        final String PROC_HARDWARE_REGEX = "Hardware\\s*:\\s*(.*)$"; /* hardware string */

        try {
            BufferedReader reader = new BufferedReader(new FileReader(FILENAME_PROC_CPUINFO));
            String cpuinfo;

            try {
                while (null != (cpuinfo = reader.readLine())) {
                    if (cpuinfo.startsWith("Hardware")) {
                        Matcher m = Pattern.compile(PROC_HARDWARE_REGEX).matcher(cpuinfo);
                        if (m.matches()) {
                            return m.group(1);
                        }
                    }
                }
                return "Unknown";
            } finally {
                reader.close();
            }
        } catch (IOException e) {
            Log.e(LOG_TAG,
                "IO Exception when getting cpuinfo for Device Info screen",
                e);

            return "Unknown";
        }
    }
    private String get32550Versino(){
    	MaxqManager mMaxNative = new MaxqManager();
    	int ret = mMaxNative.open();
        if(ret != 0)
            return null;
        byte[] response = new byte[256];
        byte[] reslen = new byte[1];
        mMaxNative.getFirmwareVersion(response, reslen);
        String version = new String(response, 0, (int) reslen[0]);
        mMaxNative.close();
    	return version;
    }
}

