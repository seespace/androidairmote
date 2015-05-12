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
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import tv.inair.airmote.connection.BaseConnection;
import tv.inair.airmote.connection.SocketClient;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends ListActivity implements BaseConnection.DeviceFoundListener {
  private ArrayAdapter<String> mAdapter;
  private SocketClient mClient;

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    cancelDiscovery();
    mClient.connectTo(devices.get(position));
    finish();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setResult(Activity.RESULT_CANCELED);

    mClient = Application.getSocketClient();
    mAdapter = new ArrayAdapter<>(this, R.layout.device_item);
    setListAdapter(mAdapter);

    setTitle("Scanning");
    Application.notify("Scanning ...", Application.Status.NORMAL);
    mClient.startScanInAir(this);
  }

  @Override
  protected void onDestroy() {
    cancelDiscovery();
    super.onDestroy();
  }

  final List<BaseConnection.Device> devices = new ArrayList<>();
  final List<String> mItems = new ArrayList<>();

  @Override
  public void onDeviceFound(BaseConnection.Device device) {
    if (!mItems.contains(device.deviceName)) {
      devices.add(device);
      mAdapter.add(device.deviceName);
    }
  }

  private void cancelDiscovery() {
    mClient.stopScanInAir();
  }
}
