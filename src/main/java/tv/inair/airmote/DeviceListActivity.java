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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tv.inair.airmote.connection.BTAdapter;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity implements AdapterView.OnItemClickListener {

  /**
   * Tag for Log
   */
  private static final String TAG = "DeviceListActivity";

  /**
   * Return Intent extra
   */
  public static final String EXTRA_DEVICE_ADDRESS = "device_address";

  public static final String EXTRA_QUICK_CONNECT = "quick_connect";

  /**
   * Newly discovered devices
   */
  private ArrayAdapter<String> mNewDevicesArrayAdapter;

  private ProgressDialog progress;
  private boolean isQuickConnect;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Intent i = getIntent();
    isQuickConnect = i.getBooleanExtra(EXTRA_QUICK_CONNECT, false);

    if (isQuickConnect) {
      progress = ProgressDialog.show(this, "", "Loading...", true);
      setVisible(false);
    }

    // Register for broadcasts when a device is discovered
    IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    // Register for broadcasts when discovery has finished
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    filter.addAction(BluetoothDevice.ACTION_UUID);
    registerReceiver(mReceiver, filter);

    // Setup the window
    //    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.activity_list);

    // Set result CANCELED in case the user backs out
    setResult(Activity.RESULT_CANCELED);

    // Initialize array adapters. One for already paired devices and
    // one for newly discovered devices
    mNewDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_item);

    // Find and set up the ListView for newly discovered devices
    ListView newDevicesListView = (ListView) findViewById(R.id.listview);
    newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
    newDevicesListView.setOnItemClickListener(this);

    doDiscovery();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    System.out.println("DeviceListActivity.onDestroy");

    if (progress.isShowing()) {
      progress.dismiss();
    }

    // Unregister broadcast listeners
    unregisterReceiver(mReceiver);

    // Make sure we're not doing discovery anymore
    if (BTAdapter.getInstance().adapter != null) {
      BTAdapter.getInstance().adapter.cancelDiscovery();
    }
  }

  /**
   * Start device discover with the BluetoothAdapter
   */
  private void doDiscovery() {
    Log.d(TAG, "doDiscovery()");

    // Indicate scanning in the title
    setProgressBarIndeterminateVisibility(true);
    setTitle("Scanning");

    // If we're already discovering, stop it
    if (BTAdapter.getInstance().adapter.isDiscovering()) {
      BTAdapter.getInstance().adapter.cancelDiscovery();
    }

    mApplicants.clear();
    mAdded.clear();
    // Request discover from BluetoothAdapter
    BTAdapter.getInstance().adapter.startDiscovery();
  }

  /**
   * The on-click listener for all devices in the ListViews
   */
  @Override
  public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
    // Cancel discovery because it's costly and we're about to connect
    BTAdapter.getInstance().adapter.cancelDiscovery();

    // Get the device MAC address, which is the last 17 chars in the View
    String info = ((TextView) v).getText().toString();
    String address = info.substring(info.length() - 17);

    // Create the result Intent and include the MAC address
    Intent intent = new Intent();
    intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

    // Set result and finish this Activity
    setResult(Activity.RESULT_OK, intent);
    finish();
  }

  List<BluetoothDevice> mApplicants = new ArrayList<>();
  List<BluetoothDevice> mAdded = new ArrayList<>();

  private void addDevice(BluetoothDevice device) {
    if (!mAdded.contains(device)) {
      mAdded.add(device);
      // If it's already paired, skip it, because it's been listed already
      if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
        mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
      }
    }
  }

  private void checkIfHasOnlyOneDevice() {
    if (mApplicants.isEmpty() & mAdded.size() == 1) {
      System.out.println("DeviceListActivity.checkIfHasOnlyOneDevice");
      // Create the result Intent and include the MAC address
      connect(mAdded.get(0));
    }
  }

  private void connect(BluetoothDevice device) {
    BTAdapter.getInstance().adapter.cancelDiscovery();
    Intent i = new Intent();
    i.putExtra(EXTRA_DEVICE_ADDRESS, device.getAddress());

    // Set result and finish this Activity
    setResult(Activity.RESULT_OK, i);
    finish();
  }

  /**
   * The BroadcastReceiver that listens for discovered devices and changes the title when
   * discovery is finished
   */
  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      // When discovery finds a device
      switch (action) {
        case BluetoothDevice.ACTION_FOUND: {
          // Get the BluetoothDevice object from the Intent
          BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          if (mApplicants.contains(device) || device.getName() == null) {
            return;
          }
          System.out.println(device.getName() + " " + Arrays.toString(device.getUuids()));
          boolean hasInAiR = BTAdapter.getInstance().checkIfInAiR(device.getUuids());
          boolean potentialInAiR = "inAiR".equals(device.getName());
          if (isQuickConnect && (hasInAiR || potentialInAiR)) {
            connect(device);
          }
          if (hasInAiR) {
            addDevice(device);
          } else if (potentialInAiR) {
            mApplicants.add(device);
          }
        }
        break;
        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
          System.out.println("FINISH");
          for (BluetoothDevice device : mApplicants) {
            device.fetchUuidsWithSdp();
          }
          checkIfHasOnlyOneDevice();
          if (mApplicants.isEmpty() && mNewDevicesArrayAdapter.isEmpty()) {
            setProgressBarIndeterminateVisibility(false);
            mNewDevicesArrayAdapter.add("No Devices");
          }
          break;
        case BluetoothDevice.ACTION_UUID: {
          setProgressBarIndeterminateVisibility(false);

          BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          mApplicants.remove(device);
          Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
          System.out.println("onReceive " + device.getName() + " " + Arrays.toString(uuids));
          if (BTAdapter.getInstance().checkIfInAiR(uuids)) {
            addDevice(device);
          }
          checkIfHasOnlyOneDevice();
          break;
        }
      }
    }
  };

}
