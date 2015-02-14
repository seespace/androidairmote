package tv.inair.airmote.connection;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

              Application.getSettingsPreferences()
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
          Proto.Event event = Helper.parseFrom((byte[]) msg.obj);
          System.out.println("READ " + event + " " + event.type);
          onEventReceived(event);
          break;
        case Constants.MESSAGE_DEVICE_NAME:
          // save the connected device's name
          mDisplayName = msg.getData().getString(Constants.DEVICE_NAME);
          break;
      }
    }
  };

  private List<WeakReference<OnEventReceived>> mEventReceiveds = new ArrayList<>();
  private List<WeakReference<OnSocketStateChanged>> mStateChangeds = new ArrayList<>();

  private String mDisplayName;
  private String mHostName;

  private BTAdapter mBtAdapter = BTAdapter.getInstance();

  public SocketClient() {
    mBtAdapter.setHandler(mHandler);
  }

  public void addEventReceivedListener(OnEventReceived listener) {
    mEventReceiveds.add(new WeakReference<>(listener));
  }

  public void addSocketStateChangedListener(OnSocketStateChanged listener) {
    mStateChangeds.add(new WeakReference<>(listener));
  }

  public boolean isConnected() {
    return mBtAdapter.isConnected();
  }

  private void notifyDisconnect(String message) {
    disconnect();
    onStateChanged(false, message);
  }

  public boolean reconnectToLastDevice() {
//    if (!Application.getSettingsPreferences().contains(HOST_NAME_KEY)) {
//      return false;
//    }
//    String lastHost = Application.getSettingsPreferences().getString(HOST_NAME_KEY, "");
//    mDisplayName = Application.getSettingsPreferences().getString(DISPLAY_NAME_KEY, "");
//    return connectTo(lastHost);
    return false;
  }

  public void reconnectToLastHost() {
    if (!Application.getTempPreferences().contains(HOST_NAME_KEY)) {
      return;
    }
    String lastHost = Application.getTempPreferences().getString(HOST_NAME_KEY, "");
    mDisplayName = Application.getTempPreferences().getString(DISPLAY_NAME_KEY, "");
    connectTo(lastHost);
  }

  public boolean connectTo(String hostName) {
    mBtAdapter.stop();
    Log.d(TAG, "ConnectTo " + hostName + " " + mDisplayName);
    mHostName = hostName;
    return mBtAdapter.connect(hostName);
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
    Iterator<WeakReference<OnEventReceived>> it = mEventReceiveds.iterator();
    while (it.hasNext()) {
      WeakReference<OnEventReceived> l = it.next();
      if (l.get() != null) {
        l.get().onEventReceived(e);
      } else {
        it.remove();
      }
    }
  }

  private void onStateChanged(boolean connect, String message) {
    Iterator<WeakReference<OnSocketStateChanged>> it = mStateChangeds.iterator();
    while (it.hasNext()) {
      WeakReference<OnSocketStateChanged> l = it.next();
      if (l.get() != null) {
        l.get().onStateChanged(connect, message);
      } else {
        it.remove();
      }
    }
  }
}
