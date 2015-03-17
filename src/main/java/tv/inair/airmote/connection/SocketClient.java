package tv.inair.airmote.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import inair.eventcenter.proto.Proto;
import tv.inair.airmote.Application;
import tv.inair.airmote.remote.Helper;

public final class SocketClient {

  private static final String TAG = "SocketClient";

  //region Description
  public static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
  public static final String USB_CONNECTED = "connected";
  public static final String USB_CONFIGURED = "configured";
  public static final String USB_FUNCTION_MASS_STORAGE = "mass_storage";
  public static final String USB_FUNCTION_ADB = "adb";
  public static final String USB_FUNCTION_MTP = "mtp";

  private boolean mUSBConnected = false;
  private boolean mIsPC = false;
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      switch (action) {
        case Intent.ACTION_POWER_CONNECTED: {
          mUSBConnected = true;
          Application.notify(context, "USB Connected");
//          Toast.makeText(context, "USB Connected", Toast.LENGTH_SHORT).show();
          quickScanAndConnect();
          break;
        }

        case Intent.ACTION_POWER_DISCONNECTED:
          mUSBConnected = false;
          break;

        case Intent.ACTION_BATTERY_CHANGED: {
          mIsPC = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == BatteryManager.BATTERY_PLUGGED_USB;
          quickScanAndConnect();
          break;
        }

//        case ACTION_USB_STATE: {
//          boolean isConnected = intent.getBooleanExtra(USB_CONNECTED, false);
//          boolean isConfigured = intent.getBooleanExtra(USB_CONFIGURED, false);
//          boolean msName = intent.getBooleanExtra(USB_FUNCTION_MASS_STORAGE, false);
//          boolean adbName = intent.getBooleanExtra(USB_FUNCTION_ADB, false);
//          boolean mtpName = intent.getBooleanExtra(USB_FUNCTION_MTP, false);
//          String des = ACTION_USB_STATE + " " + isConnected + " " + isConfigured + " " + msName + " " + adbName + " " + mtpName;
//          System.out.println(des);
//        }
      }
    }
  };

  private boolean mSettingUp = false;

  private void quickScanAndConnect() {
    if (!mIsPC || !mUSBConnected || !mSettingUp) {
      return;
    }
    Application.notify(fragment.get().getActivity(), "Connecting ...");
    mConnection.quickConnect();
  }

  public synchronized void changeToSettingMode(boolean setup) {
    mSettingUp = setup;
    if (setup) {
      mConnection.quickConnect();
    }
  }

  public boolean isInSettingMode() {
    return mSettingUp;
  }

  private WeakReference<Fragment> fragment;

  public void register(Fragment context) {
    fragment = new WeakReference<>(context);

    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
//    filter.addAction(ACTION_USB_STATE);
    context.getActivity().registerReceiver(receiver, filter);

    mConnection.register(context.getActivity());
  }

  public void unregister() {
    if (fragment != null && fragment.get() != null) {
      Fragment f = fragment.get();
      mConnection.unregister(f.getActivity());
      f.getActivity().unregisterReceiver(receiver);
      mConnection.unregister(f.getActivity());
      fragment = null;
    }
  }
  //endregion

  //region Public
  private BaseConnection mConnection;
  private BaseConnection.Device mDevice = BaseConnection.Device.EMPTY;

  public SocketClient() {
    mConnection = WifiAdapter.getInstance();
    mConnection.setHandler(new LocalHandler(this));
  }

  public String getDisplayName() {
    return mDevice.deviceName;
  }

  public boolean reconnectToLastHost() {
    BaseConnection.Device device = BaseConnection.Device.getFromPref(Application.getTempPreferences());
    return device.address != null && connectTo(device);
  }

  public boolean reconnectToLastDevice() {
    BaseConnection.Device device = BaseConnection.Device.getFromPref(Application.getSettingsPreferences());
    return device.address != null && connectTo(device);

  }

  public boolean connectTo(BaseConnection.Device device) {
    Log.d(TAG, "ConnectTo " + device.address + " " + device.deviceName);
    Application.notify(fragment.get().getActivity(), "Connecting ...");
    mDevice = device;
    return mConnection.connect(device);
  }

  public boolean isConnected() {
    return mConnection.isConnected();
  }

  public void disconnect() {
    if (!isConnected()) {
      return;
    }
    mDevice = BaseConnection.Device.EMPTY;
    mConnection.stop();
    Log.d(TAG, "Disconnect");
  }

  public void sendEvent(Proto.Event e) {
    if (!isConnected()) {
      return;
    }
    byte[] data = Helper.dataFromEvent(e);
    mConnection.write(data);
  }
  //endregion

  //region EventListener
  private List<WeakReference<OnEventReceived>> mEventReceived = new ArrayList<>();
  private List<WeakReference<OnSocketStateChanged>> mStateChanged = new ArrayList<>();

  public void addEventReceivedListener(OnEventReceived listener) {
    mEventReceived.add(new WeakReference<>(listener));
  }

  public void addSocketStateChangedListener(OnSocketStateChanged listener) {
    mStateChanged.add(new WeakReference<>(listener));
  }

  private synchronized void onEventReceived(Proto.Event e) {
    Iterator<WeakReference<OnEventReceived>> it = mEventReceived.iterator();
    while (it.hasNext()) {
      WeakReference<OnEventReceived> l = it.next();
      if (l.get() != null) {
        l.get().onEventReceived(e);
      } else {
        it.remove();
      }
    }
  }

  private synchronized void onStateChanged(boolean connect, String message) {
    Iterator<WeakReference<OnSocketStateChanged>> it = mStateChanged.iterator();
    while (it.hasNext()) {
      WeakReference<OnSocketStateChanged> l = it.next();
      if (l.get() != null) {
        l.get().onStateChanged(connect, message);
      } else {
        it.remove();
      }
    }
  }
  //endregion

  private void handleStateChange(int state) {
    switch (state) {
      case BaseConnection.STATE_CONNECTED:
        // BaseConnection.Device.saveToPref(Application.getTempPreferences(), mDevice);
        // BaseConnection.Device.saveToPref(Application.getSettingsPreferences(), mDevice);
        onStateChanged(true, mDevice.deviceName);
        Application.notify(fragment.get().getActivity(), "Connected to " + mDevice.deviceName);
        break;

      case BaseConnection.STATE_NONE:
        onStateChanged(false, mDevice.deviceName);
        Application.notify(fragment.get().getActivity(), "Disconnected");
        break;
    }
  }

  private void updateDeviceName(String name) {
    mDevice.deviceName = name;
  }

  private static class LocalHandler extends Handler {
    WeakReference<SocketClient> mClient;

    LocalHandler(SocketClient client) {
      mClient = new WeakReference<>(client);
    }

    @Override
    public void handleMessage(Message msg) {
      SocketClient client = mClient.get();
      if (client == null) {
        return;
      }
      switch (msg.what) {
        case BaseConnection.Constants.MESSAGE_STATE_CHANGE:
          client.handleStateChange(msg.arg1);
          break;

        case BaseConnection.Constants.MESSAGE_READ:
          client.onEventReceived(Helper.parseFrom((byte[]) msg.obj));
          break;

        case BaseConnection.Constants.MESSAGE_DEVICE_NAME:
          // save the connected device's name
           client.updateDeviceName(msg.getData().getString(BaseConnection.Constants.DEVICE_NAME));
          break;

        default:
          super.handleMessage(msg);
      }
    }
  }
}
