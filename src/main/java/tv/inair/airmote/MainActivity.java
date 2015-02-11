/**
 *
 * The MIT License
 *
 * Copyright (c) 2014, SeeSpace. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package tv.inair.airmote;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import inair.eventcenter.proto.Proto;
import tv.inair.airmote.bluetooth.DeviceListActivity;
import tv.inair.airmote.bluetooth.Listener;

public class MainActivity extends Activity implements OnEventReceived, OnSocketStateChanged {

  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
  private static final int REQUEST_ENABLE_BT = 3;

  private GestureControl mGestureControl;

  private NsdHelper mNsdHelper;

  private BluetoothAdapter adapter;

  private Listener mChatService = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    adapter = BluetoothAdapter.getDefaultAdapter();

    mNsdHelper = new NsdHelper(this);

    mGestureControl = new GestureControl(this, findViewById(R.id.rootView));

    Application.getSocketClient().setOnEventReceived(this);
    Application.getSocketClient().setOnSocketStateChanged(this);

    if (Application.getSocketClient().isConnected()) {
      Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

//    Intent serverIntent = new Intent(this, DeviceListActivity.class);
//    startActivityForResult(serverIntent, REQUEST_ENABLE_BT);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
  }

  Dialog mDialog;
  private void discoverInAiR() {
    System.out.println("MainActivity.discoverInAiR");
    Intent serverIntent = new Intent(this, DeviceListActivity.class);
    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    //      mNsdHelper.discoverServices();
    //      if (mDialog == null) {
    //        final View contentView = getLayoutInflater().inflate(R.layout.connect_dialog, null);
    //        final ListView listView = ((ListView) contentView.findViewById(R.id.listView));
    //        listView.setAdapter(mNsdHelper.mAdapter);
    //        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
    //          @Override
    //          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    //            System.out.println("MainActivity.onItemClick " + mNsdHelper.mAdapter.getItem(position).mHostName + " " + id);
    //          }
    //        });
    //
    //        mDialog = new AlertDialog.Builder(this)
    //            .setTitle("Connect to inAiR")
    //            .setView(contentView)
    //            .setPositiveButton("Rescan", new DialogInterface.OnClickListener() {
    //              @Override
    //              public void onClick(DialogInterface dialogInterface, int i) {
    //                mNsdHelper.stopDiscovery();
    //                mNsdHelper.discoverServices();
    //              }
    //            })
    //            .create();
    //
    //        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
    //          @Override
    //          public void onCancel(DialogInterface dialog) {
    //            mDialog = null;
    //          }
    //        });
    //
    //        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
    //          @Override
    //          public void onDismiss(DialogInterface dialog) {
    ////            if (input.getText().length() <= 0) {
    ////              mDialog.show();
    ////            } else {
    //              mDialog = null;
    ////            }
    //          }
    //        });
    //
    //        mDialog.show();
    //      }
  }

  @Override
  public void onStateChanged(boolean connect, String message) {
    if (!connect) {
      if (!adapter.isEnabled()) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
      } else {
        discoverInAiR();
      }
    } else {
//      mNsdHelper.stopDiscovery();
//      Toast.makeText(this, "Connected " + message, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    mNsdHelper.registerService();
  }

  @Override
  protected void onResume() {
    super.onResume();
//    if (!Application.getSocketClient().isConnected()) {
//      Application.getSocketClient().reconnectToLastHost();
//    }
  }

  @Override
  protected void onPause() {
    mNsdHelper.stopDiscovery();
    Application.getSocketClient().disconnect();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    mNsdHelper.tearDown();
    super.onDestroy();
  }

  private void connectDevice(Intent data, boolean secure) {
    // Get the device MAC address
    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
    // Get the BluetoothDevice object
//    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
    // Attempt to connect to the device
//    mChatService.connect(device, secure);
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CONNECT_DEVICE_SECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
          connectDevice(data, true);
        }
        break;
      case REQUEST_CONNECT_DEVICE_INSECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
          connectDevice(data, false);
        }
        break;
      case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
          // Bluetooth is now enabled, so set up a chat session
          discoverInAiR();
        } else {
          // User did not enable Bluetooth or an error occurred
          Log.d(getClass().getSimpleName(), "BT not enabled");
//          Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
//          getActivity().finish();
        }
    }
  }
}
