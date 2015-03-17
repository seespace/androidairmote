/*
 * Copyright (C) 2014 The Android Open Source Project
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

package tv.inair.airmote;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import tv.inair.airmote.connection.BaseConnection;
import tv.inair.airmote.connection.SocketClient;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity implements AdapterView.OnItemClickListener, BaseConnection.DeviceFoundListener {
  private static final String TAG = "DeviceListActivity";

  public static final String EXTRA_DEVICE_ADDRESS = "device_address";
  public static final String EXTRA_QUICK_CONNECT = "quick_connect";

  private ArrayAdapter<String> mDevicesAdapter;

  private ProgressDialog progress;
  private boolean isQuickConnect;
  private SocketClient mClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent i = getIntent();
    isQuickConnect = i.getBooleanExtra(EXTRA_QUICK_CONNECT, false);

    mClient = Application.getSocketClient();

    progress = ProgressDialog.show(this, "", "Loading...", true);
    setVisible(false);

    // Setup the window
    setContentView(R.layout.activity_list);

    // Set result CANCELED in case the user backs out
    setResult(Activity.RESULT_CANCELED);

    // Initialize array adapters. One for already paired devices and
    // one for newly discovered devices
    mDevicesAdapter = new ArrayAdapter<>(this, R.layout.device_item);

    // Find and set up the ListView for newly discovered devices
    ListView devices = (ListView) findViewById(R.id.listview);
    devices.setAdapter(mDevicesAdapter);
    devices.setOnItemClickListener(this);

    startDiscovery();
  }

  @Override
  protected void onDestroy() {
    cancelDiscovery();
    System.out.println("DeviceListActivity.onDestroy");
    if (progress.isShowing()) {
      progress.dismiss();
    }
    super.onDestroy();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  }

  @Override
  public void onDeviceFound(BaseConnection.Device device) {
    mDevicesAdapter.add(device.deviceName);
  }

  private void startDiscovery() {
    Log.d(TAG, "startDiscovery()");
    // Indicate scanning in the title
    //setProgressBarIndeterminateVisibility(true);
    setTitle("Scanning");
    Application.notify(this, "Scanning ...");

    mDevicesAdapter.clear();
    mClient.startScanInAir(this);
  }

  private void cancelDiscovery() {
    mClient.stopScanInAir();
  }

  ///**
  // * The on-click listener for all devices in the ListViews
  // */
  //@Override
  //public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
  //  // Cancel discovery because it's costly and we're about to connect
  //  cancelDiscovery();
  //
  //  // Get the device MAC address, which is the last 17 chars in the View
  //  String info = ((TextView) v).getText().toString();
  //  String address = info.substring(info.length() - 17);
  //
  //  // Create the result Intent and include the MAC address
  //  Intent intent = new Intent();
  //  intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
  //
  //  // Set result and finish this Activity
  //  setResult(Activity.RESULT_OK, intent);
  //  finish();
  //}
}
