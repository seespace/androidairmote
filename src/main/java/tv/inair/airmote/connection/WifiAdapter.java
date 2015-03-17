package tv.inair.airmote.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import tv.inair.airmote.Application;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public final class WifiAdapter extends BaseConnection {

  private static final String TAG = "WifiAdapter";

  private static final WifiConfiguration INAIR_WIFI_CONFIG = new WifiConfiguration();
  private static final String INAIR_SSID = String.format("\"%s\"", "InAir");
  private static final String INAIR_PASSWORD = String.format("\"%s\"", "12345678");

  private static final Device INAIR_DEVICE = new Device("inAiR", "192.168.49.1");
  private static final int INAIR_PORT = 8989;

  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;

  private WifiManager mManager;

  //region Singleton
  private static WifiAdapter _instance;

  public static WifiAdapter getInstance() {
    if (_instance == null) {
      _instance = new WifiAdapter();
    }
    return _instance;
  }

  private WifiAdapter() {
    INAIR_WIFI_CONFIG.SSID = INAIR_SSID;
    INAIR_WIFI_CONFIG.preSharedKey = INAIR_PASSWORD;
  }
  //endregion

  private void _connectToInAiR() {
    if (!mManager.isWifiEnabled()) {
      mManager.setWifiEnabled(true);
    }
    int netId = mManager.addNetwork(INAIR_WIFI_CONFIG);
    mManager.disconnect();
    mManager.enableNetwork(netId, false);
    mManager.reconnect();
  }

  private void _disableInAirNetwork() {
    if (!mManager.isWifiEnabled()) {
      mManager.setWifiEnabled(true);
    }
    mManager.disconnect();
    List<WifiConfiguration> configs = mManager.getConfiguredNetworks();
    for (int i = 0; i < configs.size(); i++) {
      WifiConfiguration config = configs.get(i);
      if (_checkIfInAiR(config.SSID)) {
        mManager.removeNetwork(config.networkId);
      }
    }
    mManager.reconnect();
  }

  private boolean _checkIfInAiR(String ssid) {
    return INAIR_SSID.equals(ssid);
  }

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      switch (action) {
        case WifiManager.NETWORK_STATE_CHANGED_ACTION:
          NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
          if (networkInfo.isConnected()) {
            WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            System.out.println("Connected " + wifiInfo.getSSID() + " " + Application.getSocketClient().isInSettingMode());
            if (Application.getSocketClient().isInSettingMode()) {
              if (_checkIfInAiR(wifiInfo.getSSID())) {
                connect(INAIR_DEVICE);
              } else {
                _connectToInAiR();
              }
            } else {
              _disableInAirNetwork();
            }
          } else {
            stop();
          }
          break;

        case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
          SupplicantState state = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
          System.out.println("State " + state);
          break;
      }
    }
  };

  //region Implement
  @Override
  public void register(Context context) {
    mManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
    context.registerReceiver(mReceiver, filter);
  }

  @Override
  public void unregister(Context context) {
    context.unregisterReceiver(mReceiver);
  }

  @Override
  public boolean quickConnect() {
    _connectToInAiR();
    return true;
  }

  public static void connectWifiTo(String ssid, String bssid, String capabilities, String password, boolean autoConnect) {
    WifiConfiguration config = new WifiConfiguration();
    config.SSID = "\"" + ssid + "\"";
    config.BSSID = bssid;

    if (capabilities.contains("PSK")) {
      config.preSharedKey = "\"" + password + "\"";
    } else if (capabilities.contains("WEP")) {
      config.wepKeys[0] = "\"" + password + "\"";
      config.wepTxKeyIndex = 0;
      config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
      config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    } else {
      config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
    }

    WifiAdapter ins = getInstance();

    if (!ins.mManager.isWifiEnabled()) {
      ins.mManager.setWifiEnabled(true);
    }
    int netId = ins.mManager.addNetwork(config);
    ins.mManager.disconnect();
    getInstance().mManager.enableNetwork(netId, false);
    getInstance().mManager.reconnect();
  }

  @Override
  public synchronized boolean connect(Device device) {
    if (device == null) {
      return false;
    }

    System.out.println("WifiAdapter.connect " + device);

    if (mConnectThread != null) {
      mConnectThread.cancel();
      mConnectThread = null;
    }

    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    mConnectThread = new ConnectThread(device);
    mConnectThread.start();
    setState(STATE_CONNECTING);
    return true;
  }

  @Override
  public void stop() {
    Log.d(TAG, "Stop");

    if (mConnectThread != null) {
      mConnectThread.cancel();
      mConnectThread = null;
    }

    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    setState(STATE_NONE);
  }

  @Override
  public void write(byte[] data) {
    ConnectedThread r;
    synchronized (this) {
      if (mState != STATE_CONNECTED) {
        return;
      }
      r = mConnectedThread;
    }
    r.write(data);
  }
  //endregion

  private synchronized void connected(Socket socket, Device device) {
    Log.d(TAG, "Connected " + device);

    // Cancel the thread that completed the connection
    if (mConnectThread != null) {
      mConnectThread.cancel();
      mConnectThread = null;
    }

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    // Start the thread to manage the connection and perform transmissions
    mConnectedThread = new ConnectedThread(socket);
    mConnectedThread.start();

    // Send the name of the connected device back to the UI Activity
    Message msg = mHandler.obtainMessage(BaseConnection.Constants.MESSAGE_DEVICE_NAME);
    Bundle bundle = new Bundle();
    bundle.putString(BaseConnection.Constants.DEVICE_NAME, device.deviceName);
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    setState(STATE_CONNECTED);
  }

  private class ConnectThread extends Thread {
    Device mDevice;

    public ConnectThread(Device device) {
      mDevice = device;
    }

    @Override
    public void run() {
      Log.i(TAG, "BEGIN ConnectThread " + mDevice.address);
      setName("ConnectThread");

      Socket socket;

      try {
        socket = new Socket(InetAddress.getByName(mDevice.address), INAIR_PORT);
        socket.setTcpNoDelay(true);
      } catch (IOException e) {
        e.printStackTrace();
        connectionFailed();
        return;
      }

      synchronized (WifiAdapter.this) {
        mConnectThread = null;
      }

      connected(socket, mDevice);
    }

    public void cancel() {
      interrupt();
      Log.i(TAG, "CANCEL ConnectThread ");
    }
  }

  private class ConnectedThread extends Thread {
    private final Socket mmSocket;
    private DataInputStream mDataIS;
    private DataOutputStream mDataOS;

    public ConnectedThread(Socket socket) {
      Log.d(TAG, "CREATE ConnectedThread");
      mmSocket = socket;

      // Get the Socket input and output streams
      try {
        mDataIS = new DataInputStream(socket.getInputStream());
        mDataOS = new DataOutputStream(socket.getOutputStream());
      } catch (IOException e) {
        Log.e(TAG, "temp sockets not created", e);
      }
    }

    @Override
    public void run() {
      Log.i(TAG, "BEGIN ConnectedThread");
      byte[] buffer = new byte[4096];
      int length;

      // Keep listening to the InputStream while connected
      while (!isInterrupted()) {
        try {
          if (mDataIS.available() > 0) {
            length = mDataIS.readInt();
            mDataIS.readFully(buffer, 0, length);

            // Send the obtained bytes to the UI Activity
            mHandler.obtainMessage(BaseConnection.Constants.MESSAGE_READ, length, -1, Arrays.copyOf(buffer, length))
                .sendToTarget();
          }
        } catch (IOException e) {
          Log.e(TAG, "DISCONNECTED", e);
          connectionLost();
          break;
        }
      }

      Log.e(TAG, "DISCONNECTED");
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
      try {
        mDataOS.write(buffer);
      } catch (IOException e) {
        Log.e(TAG, "Exception during write", e);
        connectionLost();
        cancel();
      }
    }

    public void cancel() {
      try {
        interrupt();
        System.out.println("ConnectedThread.cancel");
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }
}
