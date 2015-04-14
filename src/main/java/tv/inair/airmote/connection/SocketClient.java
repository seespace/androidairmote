package tv.inair.airmote.connection;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;
import tv.inair.airmote.Application;

public final class SocketClient {

  private static final String TAG = "SocketClient";

  public static final String STOP_MESSAGE = "STOP_SETUP";

  //region Description
  private boolean mUSBConnected = false;
  private boolean mIsPC = false;
  private final BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      switch (action) {
        case Intent.ACTION_POWER_CONNECTED: {
          mUSBConnected = true;
          Application.notify("USB Connected", Application.Status.SUCCESS);
          quickScanAndConnect();
          break;
        }

        case Intent.ACTION_POWER_DISCONNECTED:
          mUSBConnected = false;
          stopScanAndQuickConnect();
          break;

        case Intent.ACTION_BATTERY_CHANGED: {
          mIsPC = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == BatteryManager.BATTERY_PLUGGED_USB;
          quickScanAndConnect();
          break;
        }
      }
    }
  };

  private boolean mSettingUp = false;

  private void quickScanAndConnect() {
    if (!mIsPC || !mUSBConnected || !mSettingUp) {
      return;
    }
    quickConnect();
  }

  private void stopScanAndQuickConnect() {
    onStateChanged(false, STOP_MESSAGE);
    if (mSettingUp) {
      Application.notify("No device connect", Application.Status.ERROR);
    }
    mConnection.stopQuickConnect();
  }

  public synchronized void changeToSettingMode(boolean setup) {
    mSettingUp = setup;
    if (!setup) {
      stopScanAndQuickConnect();
    } else {
      Application.notify("Connecting ...", Application.Status.ERROR);
      mConnection.startQuickConnect();
    }
  }

  public void quickConnect() {
    Application.notify("Connecting ...", Application.Status.NORMAL);
    mConnection.startQuickConnect();
  }

  public boolean isInSettingMode() {
    return mSettingUp;
  }

  private WeakReference<Activity> activity;

  public void register(Activity context) {
    activity = new WeakReference<>(context);

    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_POWER_CONNECTED);
    filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
    context.registerReceiver(receiver, filter);

    mConnection.register(context);
  }

  public void unregister() {
    if (activity != null && activity.get() != null) {
      Activity f = activity.get();
      mConnection.unregister(f);
      f.unregisterReceiver(receiver);
      activity = null;
    }
  }
  //endregion

  //region Public
  private final BaseConnection mConnection;
  private BaseConnection.Device mDevice = BaseConnection.Device.EMPTY;

  public SocketClient() {
    mConnection = WifiAdapter.getInstance();
    mConnection.setHandler(new LocalHandler(this));
  }

  public void startScanInAir(BaseConnection.DeviceFoundListener listener) {
    mConnection.registerDeviceFoundListener(listener);
    mConnection.startScan(false);
  }

  public void stopScanInAir() {
    mConnection.registerDeviceFoundListener(null);
    mConnection.stopScan();
  }

  public void ensureNotConnectToWifiDirect() {
  }

  //public boolean reconnectToLastHost() {
  //  BaseConnection.Device device = BaseConnection.Device.getFromPref(Application.getTempPreferences());
  //  return device.address != null && connectTo(device);
  //}
  //
  //public boolean reconnectToLastDevice() {
  //  BaseConnection.Device device = BaseConnection.Device.getFromPref(Application.getSettingsPreferences());
  //  return device.address != null && connectTo(device);
  //}

  public boolean connectTo(BaseConnection.Device device) {
    Log.d(TAG, "ConnectTo " + device.address + " " + device.deviceName);
    Application.notify("Connecting ...", Application.Status.NORMAL);
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
        sendEvent(Helper.newDeviceEvent(activity.get(), Proto.DeviceEvent.REGISTER));
        onStateChanged(true, mDevice.deviceName);
        Application.notify("Connected to " + mDevice.deviceName, Application.Status.SUCCESS);
        break;

      case BaseConnection.STATE_CONNECTING:
        Application.notify("Connecting to " + mDevice.deviceName, Application.Status.NORMAL);
        break;

      case BaseConnection.STATE_NONE:
        if (!mSettingUp) {
          onStateChanged(false, mDevice.deviceName);
          Application.notify("Disconnected", Application.Status.ERROR);
        } else if (WifiAdapter.INAIR_DEVICE.deviceName.equals(mDevice.deviceName)) {
          onStateChanged(false, STOP_MESSAGE);
        }
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
