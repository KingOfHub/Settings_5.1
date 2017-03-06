package com.android.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.android.settings.R;

/**
 * Created by urovo-dotorom on 17年2月24日.
 */

public class AdminLoginDialog extends AlertDialog implements View.OnClickListener, TextWatcher {

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final OnClickListener mListener;

    private View mView;
    private EditText mPassword;
    private CheckBox mCheckBox;
    private TextView mSsid;

    public AdminLoginDialog(Context context, OnClickListener listener) {
        super(context);
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mView = getLayoutInflater().inflate(R.layout.admin_login_dialog, null);

        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        setTitle(R.string.admin_login);
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);

        mSsid = (TextView) mView.findViewById(R.id.ssid);
        mPassword = (EditText) mView.findViewById(R.id.password);
        mCheckBox = (CheckBox) mView.findViewById(R.id.show_password);

        setButton(BUTTON_SUBMIT, context.getString(R.string.admin_login_ok), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
                context.getString(R.string.admin_login_cancel), mListener);

        mSsid.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        mCheckBox.setOnClickListener(this);

        super.onCreate(savedInstanceState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (mPassword != null && mCheckBox != null) {
            mPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT | (mCheckBox.isChecked() ?
                            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                            InputType.TYPE_TEXT_VARIATION_PASSWORD));
        }
    }

    public void onClick(View view) {
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                        InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
    }

    public CharSequence getUser(){
        return mSsid.getText();
    }

    public CharSequence getPassword(){
        return mPassword.getText();
    }
}

