package com.android.settings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.util.Log;

public class DockManager {
	public static DockManager getInstance() {
		return new DockManager();
	}

	public void setEnabled(boolean enabled) {
		FileOutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream("/sys/devices/soc.0/78d9000.usb/otg_enable");
			outputStream.write(Integer.toString(enabled ? 1 : 0).getBytes());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public boolean isEnable() {
		FileInputStream inputStream = null;
		boolean result = false;
		try {
			byte[] buffer = new byte[Integer.toString(0).getBytes().length];
			inputStream = new FileInputStream("/sys/devices/soc.0/78d9000.usb/otg_enable");
			inputStream.read(buffer);
			if("1".equals(new String(buffer)))
					result = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}

}
