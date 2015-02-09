package tv.inair.airmote;

import android.os.AsyncTask;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;

public class SocketClient {
  private static final String HOST_NAME_KEY = "#hostname";
  private static final String DISPLAY_NAME_KEY = "#displayname";

  private static final String TAG = "SocketClient";

  private Socket mSocket;
  private WeakReference<OnEventReceived> mEventReceived;
  private WeakReference<OnSocketStateChanged> mStateChanged;

  private ConnectTask mConnectTask;
  private DataOutputStream mWriter;
  private DataInputStream mReader;

  private String mDisplayName;
  private String mHostName;

  public void setOnEventReceived(OnEventReceived listener) {
    mEventReceived = new WeakReference<>(listener);
  }

  public void setOnSocketStateChanged(OnSocketStateChanged listener) {
    mStateChanged = new WeakReference<>(listener);
  }

  public boolean isConnected() {
    return mSocket != null && mSocket.isConnected();
  }

  private void notifyDisconnect(String message) {
    disconnect();
    onStateChanged(false, message);
  }

  public void reconnectToLastHost() {
    if (!AiRmote.getTempPreferences().contains(HOST_NAME_KEY)) {
      return;
    }
    String lastHost = AiRmote.getTempPreferences().getString(HOST_NAME_KEY, "");
    String displayName = AiRmote.getTempPreferences().getString(DISPLAY_NAME_KEY, "");
    connectTo(lastHost, displayName);
  }

  public void connectTo(String hostName, String displayName) {
    Log.d(TAG, "ConnectTo " + hostName + " " + displayName);
    if (isConnected() || (mConnectTask != null && mConnectTask.getStatus() == AsyncTask.Status.RUNNING)) {
      return;
    }
    mDisplayName = displayName;
    mHostName = hostName;
    mConnectTask = new ConnectTask();
    mConnectTask.execute();
  }

  public void disconnect() {
    if (!isConnected()) {
      return;
    }
    mDisplayName = mHostName = null;
    try {
      mConnectTask.cancel(false);
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      mReader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      mWriter.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      mSocket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    mConnectTask = null;
    mReader = null;
    mWriter = null;
    mSocket = null;
    Log.d(TAG, "disconnect");
  }

  public void sendEvent(Proto.Event e) {
    if (!isConnected()) {
      notifyDisconnect(null);
      return;
    }
    byte[] data = Helper.dataFromEvent(e);
    try {
      mWriter.write(data);
      mWriter.flush();
    } catch (IOException e1) {
      notifyDisconnect(e1.getMessage());
    }
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

  private class Data {
    byte[] event;
    boolean connected;
    String message;

    private Data(byte[] event, boolean connected, String message) {
      this.event = event;
      this.connected = connected;
      this.message = message;
    }
  }

  // Connectivity
  private class ConnectTask extends AsyncTask<Void, Data, Void> {

    @Override
    protected Void doInBackground(Void... params) {
      try {
        InetAddress serverAddress = InetAddress.getByName(mHostName);
        mSocket = new Socket(serverAddress, 6969);
        mSocket.setTcpNoDelay(true);
        publishProgress(new Data(null, true, null));
        mWriter = new DataOutputStream(mSocket.getOutputStream());
        mReader = new DataInputStream(mSocket.getInputStream());

        while (!isCancelled()) {
          if (mReader.available() != 0) {
            byte[] event = new byte[mReader.available()];
            mReader.readFully(event);
            publishProgress(new Data(event, true, null));
          }
        }

      } catch (Exception e) {
        publishProgress(new Data(null, false, e.getMessage()));
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onProgressUpdate(Data... values) {
      Data data = values[0];
      Log.d(TAG, "ConnectTask " + data.connected + " " + data.message);
      if (data.event != null) {
//        onEventReceived(Helper.parseFrom(data.event));
      } else {
        if (data.connected) {
          // store current hostname
          AiRmote.getTempPreferences()
              .edit()
              .putString(HOST_NAME_KEY, mHostName)
              .putString(DISPLAY_NAME_KEY, mDisplayName)
              .commit();
          onStateChanged(true, mDisplayName);
        } else {
          AiRmote.getTempPreferences()
              .edit()
              .remove(HOST_NAME_KEY)
              .remove(DISPLAY_NAME_KEY)
              .commit();
          notifyDisconnect(data.message);
        }
      }
    }
  }
}
