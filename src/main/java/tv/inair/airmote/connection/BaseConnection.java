package tv.inair.airmote.connection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Parcelable;

import java.lang.ref.WeakReference;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public abstract class BaseConnection {

  public static class Device {
    public static final Device EMPTY = new Device(null, null);

    public String deviceName;
    public String address;
    public Parcelable parcelable;

    public Device(String deviceName, String address) {
      this.deviceName = deviceName;
      this.address = address;
    }

    public Device() {}

    @Override
    public String toString() {
      return "Device {" +
             "deviceName='" + deviceName + '\'' +
             ", address='" + address + '\'' +
             '}';
    }


    private static final String HOST_NAME_KEY = "#hostname";
    private static final String DISPLAY_NAME_KEY = "#displayname";

    public static void saveToPref(SharedPreferences preferences, Device device) {
      preferences.edit().putString(HOST_NAME_KEY, device.address).putString(DISPLAY_NAME_KEY, device.deviceName).apply();
    }

    public static Device getFromPref(SharedPreferences preferences) {
      Device device = new Device();
      device.address = preferences.getString(HOST_NAME_KEY, null);
      device.deviceName = preferences.getString(DISPLAY_NAME_KEY, null);
      return device;
    }
  }

  public interface DeviceFoundListener  {
    void onDeviceFound(Device device);
  }

  public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_DEVICE_NAME = 4;

    // Key names received from the BluetoothChatService Handler
    String DEVICE_NAME = "device_name";
  }

  //region Connection State
  // Constants that indicate the current connection state
  public static final int STATE_NONE = 0;       // we're doing nothing
  public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
  public static final int STATE_CONNECTED = 3;  // now connected to a remote device
  protected int mState = STATE_NONE;

  protected synchronized void setState(int state) {
    mState = state;
    mHandler.obtainMessage(BaseConnection.Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
  }

  /**
   * Indicate that the connection attempt failed and notify the UI Activity.
   */
  protected void connectionFailed() {
    setState(STATE_NONE);
  }

  protected void connectionLost() {
    setState(STATE_NONE);
  }

  public boolean isConnected() {
    return mState == STATE_CONNECTED;
  }
  //endregion

  //region Handler
  protected Handler mHandler;

  public void setHandler(Handler handler) {
    this.mHandler = handler;
  }
  //endregion

  private WeakReference<DeviceFoundListener> mDeviceFoundListener = null;

  public final void registerDeviceFoundListener(DeviceFoundListener listener) {
    if (listener != null) {
      mDeviceFoundListener = new WeakReference<>(listener);
    } else {
      mDeviceFoundListener = null;
    }
  }

  protected final void onDeviceFound(final Device device) {
    mHandler.post(new Runnable() {
      @Override
      public void run() {
        if (mDeviceFoundListener != null && mDeviceFoundListener.get() != null) {
          mDeviceFoundListener.get().onDeviceFound(device);
        }
      }
    });
  }

  public void register(Context context) {}

  public void unregister(Context context) {}

  public abstract boolean startQuickConnect();

  public abstract boolean stopQuickConnect();

  public abstract void startScan(boolean quickConnect);

  public abstract void stopScan();

  public abstract boolean connect(Device device);

  public abstract void stop();

  public abstract void write(byte[] data);
}
