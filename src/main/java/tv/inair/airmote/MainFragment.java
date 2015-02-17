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

import tv.inair.airmote.connection.BTAdapter;
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
public class MainFragment extends Fragment implements OnEventReceived, OnSocketStateChanged, GestureControl.Listener {
  private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
  private static final int REQUEST_ENABLE_BT = 2;

  //region Override parent
  private BluetoothAdapter adapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    adapter = BTAdapter.getInstance().adapter;

    Application.getSocketClient().addEventReceivedListener(this);
    Application.getSocketClient().addSocketStateChangedListener(this);

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

  private GestureControl mGestureControl;
  private View mControlView;
  private View mControlContainer;

  @Override
  public void onViewCreated(View view,
      @Nullable
      Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mGestureControl = new GestureControl(getActivity(), view.findViewById(R.id.rootView));
    mControlView = view.findViewById(R.id.controlView);
    mControlContainer = view.findViewById(R.id.controlContainer);

    view.findViewById(R.id.moreBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleControlView();
      }
    });

    view.findViewById(R.id.scanBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleScanDevices();
        hideControlView();
      }
    });

    view.findViewById(R.id.threeDBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        switchDisplayMode();
        hideControlView();
      }
    });

    view.findViewById(R.id.settingBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        settingDevice();
        hideControlView();
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    // If BT is not on, request that it be enabled.
    // setupChat() will then be called during onActivityResult
    if (!adapter.isEnabled()) {
      Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    } else {
      tryToReconnectLastDevice();
    }

    mGestureControl.setListener(this);
  }

  @Override
  public void onResume() {
    super.onResume();
    hideControlView();
  }

  @Override
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
        if (resultCode == Activity.RESULT_OK && !Application.getSocketClient().isConnected()) {
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
  //endregion

  //region Buttons
  private boolean isShow = true;

  private void toggleControlView() {
    if (isShow) {
      hideControlView();
    } else {
      showControlView();
    }
  }

  private void showControlView() {
    isShow = true;
    mControlView.animate().translationY(0).setDuration(250);
  }

  private void hideControlView() {
    isShow = false;
    mControlView.animate().translationY(mControlContainer.getHeight()).setDuration(250);
  }

  private void handleScanDevices() {
    Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
  }

  private void switchDisplayMode() {
    Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
  }

  private void settingDevice() {
    Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
  }
  //endregion

  //region Connect InAiR
  boolean isDiscovering = false;

  private void tryToReconnectLastDevice() {
    if (!Application.getSocketClient().isConnected() && !Application.getSocketClient().reconnectToLastDevice()) {
      //      discoverInAiR();
    }
  }

  private void discoverInAiR() {
    if (!adapter.isDiscovering() && !isDiscovering) {
      isDiscovering = true;
      System.out.println("MainActivity.discoverInAiR");
      Intent i = new Intent(getActivity(), DeviceListActivity.class);
      i.putExtra(DeviceListActivity.EXTRA_QUICK_CONNECT, true);
      startActivityForResult(i, REQUEST_CONNECT_DEVICE_SECURE);
    }
  }
  //endregion

  //region Implement
  @Override
  public void onEventReceived(Proto.Event event) {
    if (event != null && event.type != null) {
      switch (event.type) {
        case Proto.Event.OAUTH_REQUEST:
          Proto.OAuthRequestEvent oAuthEvent = event.getExtension(Proto.OAuthRequestEvent.event);
          Intent i = new Intent(getActivity(), WebviewActivity.class);
          i.putExtra(WebviewActivity.EXTRA_URL, oAuthEvent.authUrl);
          i.putExtra(WebviewActivity.EXTRA_REPLY_TO, event.replyTo);
          startActivity(i);
          break;
        case Proto.Event.TEXT_INPUT_REQUEST:
          //        processTextInput();
          break;

        default:
          System.out.println(event.type);
      }
    }
  }

  @Override
  public void onStateChanged(boolean connect, String message) {
    System.out.println("MainFragment.onStateChanged " + connect + " " + message);
    if (!connect) {
      if (!adapter.isEnabled()) {
        Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
      } else if (!isDiscovering) {
        tryToReconnectLastDevice();
      }
    } else {
      Toast.makeText(getActivity(), "Connected " + message, Toast.LENGTH_SHORT).show();

      Intent i = new Intent(getActivity(), WifiListActivity.class);
      startActivity(i);
    }
  }

  @Override
  public void onEvent() {
    hideControlView();
  }
  //endregion
}
