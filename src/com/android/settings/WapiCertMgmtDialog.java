package com.android.settings.wifi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import com.android.settings.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.text.method.PasswordTransformationMethod;
import android.text.method.TransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Environment;

import android.os.FileUtils;


public class WapiCertMgmtDialog extends AlertDialog implements DialogInterface.OnClickListener,
        AdapterView.OnItemSelectedListener, View.OnClickListener {

    private static final String TAG = "WapiCertMgmtDialog";

    private static final String InstallTitle = "Install";
    private static final String UninstallTitle = "Uninstall";
    public static final int MODE_INSTALL = 0;
    public static final int MODE_UNINSTALL = 1;
    private int mMode = MODE_INSTALL;
    // General views
    private View mView;

    private TextView mCreateSubdirText;
    private EditText mCreateSubdirEdit;

    private TextView mASCertText;
    private EditText mASCertEdit;

    private TextView mUserCertText;
    private EditText mUserCertEdit;

    private TextView mDeletDirText;
    private Spinner  mDeletDirSpinner;

    private String mUninstallCerts;
    private CharSequence mCustomTitle;

    private static final int INSTALL_BUTTON = BUTTON1;
    private static final int UNINSTALL_BUTTON = BUTTON3;
    private static final int CANCEL_BUTTON = BUTTON2;

    // Button positions, default to impossible values
    private int mInstallButtonPos = Integer.MAX_VALUE;
    private int mUninstallButtonPos = Integer.MAX_VALUE;
    private int mCancelButtonPos = Integer.MAX_VALUE;

    private static final String DEFAULT_CERTIFICATE_PATH =
//        "/system/wifi/wapi_certificate";
        "/data/wapi_certificate";

    private static String default_sdcard_path;
    private static String external_sdcard_path;
    private static String wifi_sdcard_path;
    private static String certificate_path;
    private static String certificate_installation_path;


    public WapiCertMgmtDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onLayout();
        super.onCreate(savedInstanceState);
    }
    private void onLayout() {
        int positiveButtonResId = 0;
        int negativeButtonResId = 0;
        int neutralButtonResId = 0;

        setInverseBackgroundForced(true);

        if (mMode == MODE_INSTALL) {
            setLayout(R.layout.wifi_wapi_cert_install);
            positiveButtonResId = R.string.wifi_wapi_cert_install_button;
            mInstallButtonPos = INSTALL_BUTTON;
        } else if (mMode == MODE_UNINSTALL) {
            setLayout(R.layout.wifi_wapi_cert_uninstall);
            neutralButtonResId = R.string.wifi_wapi_cert_uninstall_button;
            mUninstallButtonPos = UNINSTALL_BUTTON;
        }
        negativeButtonResId = R.string.wifi_wapi_cert_cancel_button;
        mCancelButtonPos = CANCEL_BUTTON;

        setButtons(positiveButtonResId, negativeButtonResId, neutralButtonResId);
    }

    private void setLayout(int layoutResId) {
        setView(mView = getLayoutInflater().inflate(layoutResId, null));
        onReferenceViews(mView);
    }

    /** Called when we need to set our member variables to point to the views. */
    private void onReferenceViews(View view) {
        if (mMode == MODE_INSTALL) {
            mCreateSubdirText = (TextView)view.findViewById(R.id.wapi_cert_create_subdir_text);
            mCreateSubdirEdit = (EditText)view.findViewById(R.id.wapi_cert_create_subdir_edit);

            mASCertText = (TextView)view.findViewById(R.id.wapi_as_cert_text);
            mASCertEdit = (EditText)view.findViewById(R.id.wapi_as_cert_edit);

            mUserCertText = (TextView)view.findViewById(R.id.wapi_user_cert_text);
            mUserCertEdit = (EditText)view.findViewById(R.id.wapi_user_cert_edit);
        } else if (mMode == MODE_UNINSTALL) {
            mDeletDirText = (TextView)view.findViewById(R.id.wifi_wapi_cert_delet_subdir_text);
            mDeletDirSpinner = (Spinner)view.findViewById(R.id.wifi_wapi_cert_delet_subdir_spinner);
            mDeletDirSpinner.setOnItemSelectedListener(this);
            setDeletDirSpinnerAdapter();
        }
    }
    public void setMode(int mode) {
        mMode = mode;
    }

    private void setButtons(int positiveResId, int negativeResId, int neutralResId) {

        if (positiveResId > 0) {
            setButton(getContext().getString(positiveResId), this);
        }

        if (neutralResId > 0) {
            setButton3(getContext().getString(neutralResId), this);
        }

        if (negativeResId > 0) {
            setButton2(getContext().getString(negativeResId), this);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        Log.v(TAG, "onClick which " + which);
        if (which == mInstallButtonPos) {
            handleInstall();
        } else if (which == mUninstallButtonPos) {
            handleUninstall();
        } else if (which == mCancelButtonPos) {
            handleCancle();
        }
    }

    private void handleInstall() {
        Log.v(TAG, "handleInstall");

        String stringDefDir = DEFAULT_CERTIFICATE_PATH;
        File defDir = new File(stringDefDir);
        if (!defDir.exists()) {
            defDir.mkdir();
            if (!defDir.exists()) {
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.error_title)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage("Cert. base dir create failed")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }
            FileUtils.setPermissions(stringDefDir, FileUtils.S_IRWXU| FileUtils.S_IRWXG | FileUtils.S_IRWXO , -1, -1);
        }

	String subdir = getInput(mCreateSubdirEdit);
        if (null == subdir || TextUtils.isEmpty(subdir)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_subdir_name_is_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        String stringDestDir = DEFAULT_CERTIFICATE_PATH + "/" + subdir;
        File destDir = new File(stringDestDir);
        if (destDir.exists()) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_subdir_exist)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        try {
            destDir.mkdir();
        } catch (Exception e) {
            setMessage(e.toString());
        }
        if (!destDir.exists()) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_subdir_create_fail)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String asCert = getInput(mASCertEdit);
        if (null == asCert || TextUtils.isEmpty(asCert)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_as_name_is_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            deleteAll(stringDestDir);
            return;
        }

        wifi_sdcard_path = "/system/wifi/sdcard";
        external_sdcard_path = System.getenv("SECONDARY_STORAGE");
        default_sdcard_path = Environment.getMediaStorageDirectory().toString();

        certificate_installation_path = default_sdcard_path;
        Log.d(TAG, "default_sdcard_path: " + default_sdcard_path );
        Log.d(TAG, "asCert file:" + asCert);
        certificate_path = default_sdcard_path + "/" + asCert;
        Log.d(TAG, "certificate_path: " + certificate_path );
        File fileASCert = new File(certificate_path);
        Log.d(TAG, "fileASCert.exists(): " + fileASCert.exists() );

        if (!fileASCert.exists()) {
            Log.d(TAG, "Certificate path: " + certificate_path + " does not exist");
            Log.d(TAG, "Hence trying with " + external_sdcard_path);
            certificate_installation_path = external_sdcard_path;
            certificate_path = external_sdcard_path + "/" + asCert;
            fileASCert = new File(certificate_path);
            Log.d(TAG, "fileASCert.exists(): " + fileASCert.exists() );

            if (!fileASCert.exists()) {
                Log.d(TAG, "Secondary certificate path: " + certificate_path + " does not exist.");
                Log.d(TAG, "Hence trying with " + wifi_sdcard_path);
                certificate_installation_path = wifi_sdcard_path;
                certificate_path = wifi_sdcard_path + "/" + asCert;
                fileASCert = new File(certificate_path);
                Log.d(TAG, "fileASCert.exists(): " + fileASCert.exists() );

                if (!fileASCert.exists()) {
                    Log.d(TAG, "wifi certificate path: " + certificate_path + " does not exist.");
                    Log.d(TAG, "Hence ABORTING!!!!!");
                    new AlertDialog.Builder(getContext())
                            .setTitle(R.string.error_title)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setMessage(R.string.wifi_wapi_cert_mgmt_as_dont_exist)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    deleteAll(stringDestDir);
                    return;
                }
            }
        }

        /* Assuming that all the certificates will be in single path */
        Log.e(TAG, "certificate is installing from " + certificate_installation_path);

        if (!isAsCertificate(certificate_installation_path + "/" + asCert)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_as_format_is_wrong)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            deleteAll(stringDestDir);
            return;
        }
        Log.e(TAG, "handleInstall Create AS Cert: = " + asCert);
        File fileDestAS = new File(stringDestDir + "/" + "as.cer");
        try {
            fileDestAS.createNewFile();
        } catch (Exception e) {
            setMessage(e.toString());
        }
        if (fileDestAS.exists()) {
            if (!copyFile(fileDestAS, fileASCert)) {
                deleteAll(stringDestDir);
                return;
            }
        } else {
            deleteAll(stringDestDir);
            return;
        }

        String userCert = getInput(mUserCertEdit);
        if (null == userCert || TextUtils.isEmpty(userCert)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_user_name_is_empty)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            deleteAll(stringDestDir);
            return;
        }
        File fileUserCert = new File(certificate_installation_path + "/" + userCert);
        if (!fileUserCert.exists()) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_user_dont_exist)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            deleteAll(stringDestDir);
            return;
        }
        if (!isUserCertificate(certificate_installation_path + "/" + userCert)) {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.error_title)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(R.string.wifi_wapi_cert_mgmt_user_format_is_wrong)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            deleteAll(stringDestDir);
            return;
        }
        File fileDestUser = new File(stringDestDir + "/" + "user.cer");
        try {
            fileDestUser.createNewFile();
        } catch (Exception e) {
            setMessage(e.toString());
        }
        if (fileDestUser.exists()) {
            if (!copyFile(fileDestUser, fileUserCert)) {
                deleteAll(stringDestDir);
                return;
            }
        } else {
            deleteAll(stringDestDir);
            return;
        }
//      FileUtils.setPermissions(stringDefDir, FileUtils.S_IRWXU| FileUtils.S_IRWXG | FileUtils.S_IRWXO , -1, -1);
        FileUtils.setPermissions(stringDestDir, FileUtils.S_IRWXU| FileUtils.S_IRWXO , -1, -1);
        FileUtils.setPermissions(stringDestDir + "/" + "user.cer", FileUtils.S_IRUSR | FileUtils.S_IRGRP | FileUtils.S_IROTH , -1, -1);
        FileUtils.setPermissions(stringDestDir + "/" + "as.cer", FileUtils.S_IRUSR | FileUtils.S_IRGRP | FileUtils.S_IROTH , -1, -1);
    }

    private boolean copyFile(File fileDest, File fileSource) {
        FileInputStream fI;
        FileOutputStream fO;
        byte[] buf = new byte[1024];
        int i = 0;
        Log.v(TAG, "copyFile");
        try {
            fI = new FileInputStream(fileSource);
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            fO = new FileOutputStream(fileDest);
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        while (true) {
            try {
                i = fI.read(buf);
            } catch (Exception e) {
                setMessage(e.toString());
                return false;
            }
            if (i == -1) {
                break;
            }
            try {
                fO.write(buf, 0, i);
            } catch (Exception e) {
                setMessage(e.toString());
                return false;
            }
        }
        try {
            fI.close();
            fO.close();
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        return true;
    }

    public int searchString(String find_str, File file) throws Exception {
        FileReader reader = new FileReader(file);
        BufferedReader reader2 = new BufferedReader(reader, 2048);
        String s = "";
        String buffer = new String("");
        do {
            buffer += s;
        }while(( s = reader2.readLine()) != null);
        return buffer.split(find_str).length - 1;
    }

    private boolean isAsCertificate(String ascert) {
        String stringCertBegin = "BEGIN CERTIFICATE";
        String stringCertEnd = "END CERTIFICATE";
        String stringECBegin = "BEGIN EC PRIVATE KEY";
        String stringECEnd = "END EC PRIVATE KEY";
        File as = new File(ascert);
        try {
            if (1 != searchString(stringCertBegin, as)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (1 != searchString(stringCertEnd, as)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (searchString(stringECBegin, as) != 0) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (searchString(stringECEnd, as) != 0) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        return true;
    }

    private boolean isUserCertificate(String usercert) {
        String stringCertBegin = "BEGIN CERTIFICATE";
        String stringCertEnd = "END CERTIFICATE";
        String stringECBegin = "BEGIN EC PRIVATE KEY";
        String stringECEnd = "END EC PRIVATE KEY";
        File user = new File(usercert);
        try {
            if (1 != searchString(stringCertBegin, user)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (1 != searchString(stringCertEnd, user)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (1 != searchString(stringECBegin, user)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        try {
            if (1 != searchString(stringECEnd, user)) {
                return false;
            }
        } catch (Exception e) {
            setMessage(e.toString());
            return false;
        }
        return true;
    }

    private String getInput(EditText edit) {
        return (edit != null) ? (edit.getText().toString()) : null;
    }

    private void handleUninstall() {
        Log.v(TAG, "handleUninstall");
        if (null != mUninstallCerts) {
            deleteAll(mUninstallCerts);
        }
    }

    private void setDeletDirSpinnerAdapter() {
        Context context = getContext();
        File certificateList [];
        ArrayList<String> cerString= new ArrayList<String>();
        int i;

        //find all certificate
        File certificatePath = new File(DEFAULT_CERTIFICATE_PATH);
        try {
            if (!certificatePath.isDirectory()) {
                return;
            }

            //build string array
            certificateList = certificatePath.listFiles();
            for(i=0; i < certificateList.length; i++){
                cerString.add(certificateList[i].getName());
            }

            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                                   android.R.layout.simple_spinner_item,
                                   (String [])cerString.toArray(new String[0]));
                                   adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                   mDeletDirSpinner.setAdapter(adapter);

        } catch (Exception e) {
            setMessage(e.toString());
        }
    }

    private int getDeletDirFromSpinner() {
        int position = mDeletDirSpinner.getSelectedItemPosition();
        return position;
    }

    private void handleDeletDirChange(int deletDirIdx) {
        File certificateList [];

        //find all certificate
        File certificatePath = new File(DEFAULT_CERTIFICATE_PATH);
        try{
            if (!certificatePath.isDirectory()){
                return;
            } else {
                certificateList = certificatePath.listFiles();
                mUninstallCerts = certificateList[deletDirIdx].getAbsolutePath();
            }
        } catch (Exception e){
            setMessage(e.toString());
        }
    }

    private void deleteAll(String filepath) {
        File f = new File(filepath);
        Log.v(TAG, "deleteAll filepath " + filepath);

        if (f.exists() && f.isDirectory()) {
            File delFile[] = f.listFiles();
            int fileNum = delFile.length;
            int i;
            if (fileNum == 0) {
                f.delete();
            } else {
                for (i = 0; i < fileNum; i++) {
                    String subdirectory = delFile[i].getAbsolutePath();
                    deleteAll(subdirectory);
                }
            }
            f.delete();
        } else if (f.exists()) {
            f.delete();
        }
    }

    private void handleCancle() {

    }

    public void onClick(View v) {
        Log.v(TAG, "onClick View ");
    }

    public void onNothingSelected(AdapterView parent) {
        Log.v(TAG, "onNothingSelected ");
    }

    public void onItemSelected(AdapterView parent, View view, int position, long id) {
        Log.v(TAG, "onItemSelected ");
        if (parent == mDeletDirSpinner) {
            handleDeletDirChange(getDeletDirFromSpinner());
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        mCustomTitle = title;
    }

    @Override
    public void setTitle(int titleId) {
        setTitle(getContext().getString(titleId));
    }
}
