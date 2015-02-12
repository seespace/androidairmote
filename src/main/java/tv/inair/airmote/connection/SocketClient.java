package tv.inair.airmote.connection;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

import tv.inair.airmote.Application;
import tv.inair.airmote.remote.Helper;
import tv.inair.airmote.remote.Proto;

public class SocketClient {
  private static final String HOST_NAME_KEY = "#hostname";
  private static final String DISPLAY_NAME_KEY = "#displayname";

  private static final String TAG = "SocketClient";

  private final Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case Constants.MESSAGE_STATE_CHANGE:
          switch (msg.arg1) {
            case BTAdapter.STATE_CONNECTED:
              System.out.println("SocketClient.handleMessage SAVED");
              Application.getTempPreferences()
                  .edit()
                  .putString(HOST_NAME_KEY, mHostName)
                  .putString(DISPLAY_NAME_KEY, mDisplayName)
                  .commit();
              onStateChanged(true, mDisplayName);
              break;
            case BTAdapter.STATE_NONE:
              onStateChanged(false, mDisplayName);
              break;
          }
          break;
        case Constants.MESSAGE_READ:
          byte[] data = (byte[]) msg.obj;
          int length = msg.arg1;
          System.out.println("SocketClient.handleMessage " + length);
          onEventReceived(Helper.parseFrom(data));
          break;
        case Constants.MESSAGE_DEVICE_NAME:
          // save the connected device's name
          mDisplayName = msg.getData().getString(Constants.DEVICE_NAME);
          break;
        case Constants.MESSAGE_TOAST:
          System.out.println("SocketClient.handleMessage " + msg.getData().getString(Constants.TOAST));
          break;
      }
    }
  };

  //  private Socket mSocket;
  private WeakReference<OnEventReceived> mEventReceived;
  private WeakReference<OnSocketStateChanged> mStateChanged;

  private String mDisplayName;
  private String mHostName;

  private BTAdapter mBtAdapter = BTAdapter.getInstance();

  public SocketClient() {
    mBtAdapter.setHandler(mHandler);
  }

  public void setOnEventReceived(OnEventReceived listener) {
    mEventReceived = new WeakReference<>(listener);
  }

  public void setOnSocketStateChanged(OnSocketStateChanged listener) {
    mStateChanged = new WeakReference<>(listener);
  }

  public boolean isConnected() {
    return mBtAdapter.isConnected();
  }

  private void notifyDisconnect(String message) {
    disconnect();
    onStateChanged(false, message);
  }

  public void reconnectToLastHost() {
    if (!Application.getTempPreferences().contains(HOST_NAME_KEY)) {
      return;
    }
    String lastHost = Application.getTempPreferences().getString(HOST_NAME_KEY, "");
    mDisplayName = Application.getTempPreferences().getString(DISPLAY_NAME_KEY, "");
    connectTo(lastHost);
  }

  public void connectTo(String hostName) {
    Log.d(TAG, "ConnectTo " + hostName + " " + mDisplayName);
    mHostName = hostName;
    mBtAdapter.connect(hostName);
  }

  public void disconnect() {
    if (!isConnected()) {
      return;
    }
    mDisplayName = mHostName = null;
    mBtAdapter.stop();
    Log.d(TAG, "disconnect");
  }

  public void sendEvent(Proto.Event e) {
    if (!isConnected()) {
      notifyDisconnect(null);
      return;
    }
    byte[] data = Helper.dataFromEvent(e);
    mBtAdapter.write(data);
  }

  private void onEventReceived(Proto.Event e) {
    if (mEventReceived.get() != null) {
      mEventReceived.get().onEventReceived(e);
    }
  }

  private void onStateChanged(boolean connect, String message) {
    if (mStateChanged.get() != null) {
      mStateChanged.get().onStateChanged(connect, message);
    }
  }
}
