package tv.inair.airmote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.OnSocketStateChanged;
import tv.inair.airmote.remote.GestureControl;
import tv.inair.airmote.remote.Proto;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class MainFragment extends Fragment implements OnEventReceived, OnSocketStateChanged {
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_ENABLE_BT = 2;

  private GestureControl mGestureControl;

  private BluetoothAdapter adapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    adapter = BluetoothAdapter.getDefaultAdapter();

    Application.getSocketClient().setOnEventReceived(this);
    Application.getSocketClient().setOnSocketStateChanged(this);

    if (Application.getSocketClient().isConnected()) {
      Toast.makeText(getActivity(), "Connected", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
      @Nullable
      ViewGroup container,
      @Nullable
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_main, container, false);
  }

  @Override
  public void onViewCreated(View view,
      @Nullable
      Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mGestureControl = new GestureControl(getActivity(), view.findViewById(R.id.rootView));
  }

  @Override
  public void onStart() {
    super.onStart();
    // If BT is not on, request that it be enabled.
    // setupChat() will then be called during onActivityResult
    if (!adapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      // Otherwise, setup the chat session
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    //    if (!Application.getSocketClient().isConnected()) {
    //      Application.getSocketClient().reconnectToLastHost();
    //    }

    // Performing this check in onResume() covers the case in which BT was
    // not enabled during onStart(), so we were paused to enable it...
    // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
//    if (mChatService != null) {
      // Only if the state is STATE_NONE, do we know that we haven't started already
      //      if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
      //        // Start the Bluetooth chat services
      //        mChatService.start();
      //      }
//    }
  }

  @Override
  public void onPause() {
    Application.getSocketClient().disconnect();
    super.onPause();
  }

  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_CONNECT_DEVICE_SECURE:
        // When DeviceListActivity returns with a device to connect
        if (resultCode == Activity.RESULT_OK) {
          String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
          Application.getSocketClient().connectTo(address);
        }
        isDiscovering = false;
        break;

      case REQUEST_ENABLE_BT:
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
          // Bluetooth is now enabled, so set up a chat session
          discoverInAiR();
        } else {
          isDiscovering = false;
          // User did not enable Bluetooth or an error occurred
          Log.d(getClass().getSimpleName(), "BT not enabled");
          Toast.makeText(getActivity(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
        }
    }
  }

  @Override
  public void onEventReceived(Proto.Event event) {
  }

  boolean isDiscovering = false;

  private void discoverInAiR() {
    if (!adapter.isDiscovering() && !isDiscovering) {
      isDiscovering = true;
      System.out.println("MainActivity.discoverInAiR");
      Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
      startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    }
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
      Toast.makeText(getActivity(), "Connected " + message, Toast.LENGTH_SHORT).show();
    }
  }
}
