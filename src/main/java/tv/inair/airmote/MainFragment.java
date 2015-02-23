package tv.inair.airmote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import tv.inair.airmote.connection.BTAdapter;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.OnSocketStateChanged;
import tv.inair.airmote.remote.GestureControl;
import tv.inair.airmote.remote.Proto;
import tv.inair.airmote.utils.BitmapHelper;

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
      Toast.makeText(getActivity(), "Connected " + Application.getSocketClient().getDisplayName(), Toast.LENGTH_SHORT).show();
    } else {
      Application.getSocketClient().reconnectToLastDevice();
    }

    setupListener();
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
  private View mRootView;
  private View mControlView;
  private View mControlContainer;
  private ImageView mGuideImage;

  @Override
  public void onViewCreated(View view,
      @Nullable
      Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mRootView = view.findViewById(R.id.rootView);
    mControlView = view.findViewById(R.id.controlView);
    mControlContainer = view.findViewById(R.id.controlContainer);
    mGuideImage = ((ImageView) view.findViewById(R.id.imageView));

    final ImageView moreBtn = ((ImageView) view.findViewById(R.id.moreBtn));
    final ImageView scan = ((ImageView) view.findViewById(R.id.scan));
    final ImageView mode2d3d = ((ImageView) view.findViewById(R.id.mode2d3d));
    final ImageView settings = ((ImageView) view.findViewById(R.id.settings));

    mGestureControl = new GestureControl(getActivity(), mRootView);

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

    ViewTreeObserver vto = mRootView.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        ViewTreeObserver obs = mRootView.getViewTreeObserver();
        obs.removeOnGlobalLayoutListener(this);

        BitmapHelper.loadImageIntoView(getResources(), R.drawable.remote_guide, mGuideImage);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.more, moreBtn);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.mode2d3d, mode2d3d);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.scan, scan);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.settings, settings);

        System.out.println("MainFragment " + MainFragment.this);

        if (!Application.getSettingsPreferences().contains(Application.FIRST_TIME_KEY)) {
          mGuideImage.setVisibility(View.VISIBLE);
          mOnSettingUp = true;
          Application.getSettingsPreferences().edit().putBoolean(Application.FIRST_TIME_KEY, false).apply();
        } else {
          mOnSettingUp = false;
          mGuideImage.setVisibility(View.GONE);
        }

        hideControlView();
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    // If BT is not on, request that it be enabled.
    if (!adapter.isEnabled()) {
      requestEnableBT();
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
  public void onDestroy() {
    if (adapter.isDiscovering()) {
      adapter.cancelDiscovery();
    }
    getActivity().unregisterReceiver(mUSBReceiver);
    getActivity().unregisterReceiver(mBluetoothReceiver);

    super.onDestroy();
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
        isRequesting = false;
        // When the request to enable Bluetooth returns
        if (resultCode == Activity.RESULT_OK) {
          if (mOnSettingUp) {
            quickScanAndConnect();
          }
        } else {
          isDiscovering = false;
          // User did not enable Bluetooth or an error occurred
          Log.d(getClass().getSimpleName(), "BT not enabled");
          Toast.makeText(getActivity(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
        }
    }
  }

  public boolean onBackPressed() {
    if (mGuideImage.getVisibility() == View.VISIBLE) {
      mGuideImage.setVisibility(View.GONE);
      mOnSettingUp = false;
      return true;
    }
    return false;
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
    mOnSettingUp = false;
    discoverInAiR();
  }

  private void switchDisplayMode() {
    Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
  }

  private void settingDevice() {
    Application.getSocketClient().disconnect();
    if (adapter.isDiscovering()) {
      adapter.cancelDiscovery();
    }
    setState(STATE_NONE);
    mOnSettingUp = true;
    mIsPC = false;
    mGuideImage.setVisibility(View.VISIBLE);

    if (!adapter.isEnabled()) {
      requestEnableBT();
    }
  }
  //endregion

  static final int STATE_NONE = 0;
  static final int STATE_SCANNING = 1;
  static final int STATE_CONNECTING = 2;
  static final int STATE_CONNECTED = 3;

  private int mState;

  private void setState(int state) {
    mState = state;
  }

  private boolean mOnSettingUp = false;

  private void setupListener() {
    setState(STATE_NONE);

    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);

    getActivity().registerReceiver(mUSBReceiver, filter);

    // Register for broadcasts when a device is discovered
    filter = new IntentFilter();
    filter.addAction(BluetoothDevice.ACTION_FOUND);
    filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
    getActivity().registerReceiver(mBluetoothReceiver, filter);
  }

  private boolean mUSBConnected = false;
  private boolean mIsPC = false;
  private final BroadcastReceiver mUSBReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      switch (action) {
        case Intent.ACTION_POWER_CONNECTED: {
          mUSBConnected = true;
          Toast.makeText(getActivity(), "USB Connected", Toast.LENGTH_SHORT).show();
          quickScanAndConnect();
          break;
        }

        case Intent.ACTION_POWER_DISCONNECTED:
          mUSBConnected = false;
          adapter.cancelDiscovery();
          if (mOnSettingUp) {
            Toast.makeText(getActivity(), "Please connect this phone to inAiR device", Toast.LENGTH_LONG).show();
          }
          break;

        case Intent.ACTION_BATTERY_CHANGED: {
          mIsPC = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == BatteryManager.BATTERY_PLUGGED_USB;
          quickScanAndConnect();
          break;
        }
      }
    }
  };

  private boolean isRequesting = false;
  private void requestEnableBT() {
    if (isRequesting) {
      return;
    }
    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    startActivityForResult(i, REQUEST_ENABLE_BT);
  }

  private void quickScanAndConnect() {
    if (!mIsPC || !mUSBConnected || !adapter.isEnabled()) {
      return;
    }

    setState(STATE_SCANNING);
    adapter.startDiscovery();
  }

  /**
   * The BroadcastReceiver that listens for discovered devices and changes the title when
   * discovery is finished
   */
  private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (!mOnSettingUp) {
        return;
      }
      String action = intent.getAction();

      // When discovery finds a device
      switch (action) {
        case BluetoothDevice.ACTION_FOUND: {
          // Get the BluetoothDevice object from the Intent
          BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
          if (device.getName() == null) {
            return;
          }
          boolean hasInAiR = BTAdapter.getInstance().checkIfInAiR(device.getUuids());
          boolean potentialInAiR = "inAiR".equals(device.getName());
          if (hasInAiR || potentialInAiR) {
            adapter.cancelDiscovery();
//            Toast.makeText(getActivity(), "Connecting " + device.getAddress(), Toast.LENGTH_SHORT).show();
            setState(STATE_CONNECTING);
            Application.getSocketClient().connectTo(device.getAddress());
          }
          break;
        }
        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
          if (mState < STATE_CONNECTING) {
            Toast.makeText(getActivity(), "Have no inAiR around ", Toast.LENGTH_SHORT).show();
          }
          break;
      }
    }
  };

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
      if (mOnSettingUp) {
        adapter.startDiscovery();
      } else {
        Intent i = new Intent(getActivity(), DeviceListActivity.class);
        startActivityForResult(i, REQUEST_CONNECT_DEVICE_SECURE);
      }
    }
  }
  //endregion

  //region Implement
  @Override
  public void onEventReceived(Proto.Event event) {
    if (!isAdded()) {
      return;
    }
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
    if (!isAdded()) {
      return;
    }
    System.out.println("MainFragment.onStateChanged " + connect + " " + message);
    if (!connect) {
      if (mOnSettingUp) {
        mOnSettingUp = false;
        Toast.makeText(getActivity(), "Please try again" + message, Toast.LENGTH_SHORT).show();
      }
    } else {
      if (getActivity() != null) {
        Toast.makeText(getActivity(), "Connected " + message, Toast.LENGTH_SHORT).show();

        mGuideImage.setVisibility(View.GONE);
        setState(STATE_CONNECTED);
        if (mOnSettingUp) {
          mOnSettingUp = false;
          Intent i = new Intent(getActivity(), WifiListActivity.class);
          startActivity(i);
        }
      }
    }
  }

  @Override
  public void onEvent() {
    hideControlView();
  }
  //endregion
}
