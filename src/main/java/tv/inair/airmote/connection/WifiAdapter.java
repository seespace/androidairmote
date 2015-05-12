package tv.inair.airmote.connection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    INAIR_WIFI_CONFIG.status = WifiConfiguration.Status.DISABLED;
    INAIR_WIFI_CONFIG.priority = 40;

    INAIR_WIFI_CONFIG.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
    INAIR_WIFI_CONFIG.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
    INAIR_WIFI_CONFIG.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
    INAIR_WIFI_CONFIG.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
    INAIR_WIFI_CONFIG.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
    INAIR_WIFI_CONFIG.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
    INAIR_WIFI_CONFIG.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
    INAIR_WIFI_CONFIG.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
    INAIR_WIFI_CONFIG.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

    INAIR_WIFI_CONFIG.preSharedKey = INAIR_PASSWORD;

    mNsdHelper = new NSDHelper();
  }
  //endregion

  //region Wifi
  private static final WifiConfiguration INAIR_WIFI_CONFIG = new WifiConfiguration();
  private static final String INAIR_SSID = String.format("\"%s\"", "InAiR");
  private static final String INAIR_PASSWORD = String.format("\"%s\"", "12345678");

  public static final Device INAIR_DEVICE = new Device("InAiR", "192.168.49.1");
  private static final int INAIR_PORT = 8989;

  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;

  private WifiManager mManager;
  private NSDHelper mNsdHelper;

  private SocketClient mClient;

  private boolean mFindWifi = false;
  private boolean mConnectInAiR = false;

  public static boolean isConnecting(SupplicantState state) {
    switch(state) {
      case AUTHENTICATING:
      case ASSOCIATING:
      case ASSOCIATED:
      case FOUR_WAY_HANDSHAKE:
      case GROUP_HANDSHAKE:
      case COMPLETED:
        return true;
      case DISCONNECTED:
      case INTERFACE_DISABLED:
      case INACTIVE:
      case SCANNING:
      case DORMANT:
      case UNINITIALIZED:
      case INVALID:
        return false;
      default:
        throw new IllegalArgumentException("Unknown supplicant state");
    }
  }

  private void _connectToInAiR() {
    if (!mSetupInAiR) {
      stop();
      mSetupInAiR = true;
      if (!mManager.isWifiEnabled()) {
        mManager.setWifiEnabled(true);
      }
      mNsdHelper.stopDiscover();

      WifiInfo wifiInfo = mManager.getConnectionInfo();
      if (wifiInfo != null && isConnecting(wifiInfo.getSupplicantState()) && _checkIfInAiR(wifiInfo.getSSID())) {
        System.out.println("WifiAdapter._connectToInAiR");
        connect(INAIR_DEVICE);
      } else {
        mFindWifi = true;
        mManager.startScan();
      }
    }
  }

  private void _disableInAirNetwork() {
    if (INAIR_WIFI_CONFIG.networkId != -1) {
      mSetupInAiR = false;
      mNsdHelper.stopDiscover();
      mManager.setWifiEnabled(true);
      mManager.disconnect();
      mManager.removeNetwork(INAIR_WIFI_CONFIG.networkId);
      for (WifiConfiguration wifi : mManager.getConfiguredNetworks()) {
        mManager.enableNetwork(wifi.networkId, false);
      }
      mManager.reconnect();
      mManager.saveConfiguration();
      INAIR_WIFI_CONFIG.networkId = -1;
    }
  }

  private boolean _checkIfInAiR(String ssid) {
    return INAIR_SSID.equals(ssid);
  }

  private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();

      switch (action) {
        case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
          if (mSetupInAiR && mFindWifi) {
            //System.out.println(" ** " + action);
            List<ScanResult> results = mManager.getScanResults();
            for (ScanResult res : results) {
              if (_checkIfInAiR("\"" + res.SSID + "\"")) {
                mFindWifi = false;
                int netId = mManager.addNetwork(INAIR_WIFI_CONFIG);
                if (netId != -1) {
                  INAIR_WIFI_CONFIG.networkId = netId;
                  mManager.disconnect();
                  mManager.enableNetwork(netId, true);
                } else {
                  Log.d(TAG, "WTF");
                }
                //System.out.println("FOUND INAIR " + netId + " " + INAIR_WIFI_CONFIG.networkId);
                break;
              }
            }
          }
          break;

        case WifiManager.NETWORK_STATE_CHANGED_ACTION:
          NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
          if (networkInfo.isConnected()) {
            if (mSetupInAiR) {
              if (!mFindWifi) {
                WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                //System.out.println(" ++ " + action + " " + mFindWifi + " " + wifiInfo.getSSID() + " " + mConnectInAiR);
                if ("<unknown ssid>".equals(wifiInfo.getSSID())) {
                  return;
                }
                if (_checkIfInAiR(wifiInfo.getSSID())) {
                  if (!mConnectInAiR) {
                    mConnectInAiR = true;
                    mClient.connectTo(INAIR_DEVICE);
                  }
                } else {
                  mFindWifi = true;
                  mManager.startScan();
                }
              }
            } else if (mQuickConnect) {
              mNsdHelper.startDiscover();
            }
          }
          break;
      }
    }
  };

  private synchronized void connected(Socket socket, Device device) {
    Log.d(TAG, "Connected " + device);

    mNsdHelper.stopDiscover();

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

    setState(STATE_CONNECTED);

    // Send the name of the connected device back to the UI Activity
    Bundle bundle = new Bundle();
    bundle.putString(BaseConnection.Constants.DEVICE_NAME, device.deviceName);
    Message msg = mHandler.obtainMessage(BaseConnection.Constants.MESSAGE_DEVICE_NAME);
    msg.setData(bundle);
    mHandler.sendMessage(msg);
  }
  //endregion

  //region Implement

  @Override
  public void register(Context context) {
    mClient = Application.getSocketClient();
    mNsdHelper.registerListener(context);
    mManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

    IntentFilter filter = new IntentFilter();
    filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
    filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    context.registerReceiver(mReceiver, filter);

    if (!mManager.isWifiEnabled()) {
      mManager.setWifiEnabled(true);
    }
  }

  @Override
  public void unregister(Context context) {
    mNsdHelper.unregisterListener(context);
    try {
      context.unregisterReceiver(mReceiver);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean mQuickConnect = false;
  private boolean mSetupInAiR = false;

  @Override
  public boolean startQuickConnect() {
    if (mClient == null) {
      Log.w(TAG, "WTF???");
      return false;
    }
    if (mClient.isInSettingMode()) {
      _connectToInAiR();
    } else {
      mQuickConnect = true;
    }
    return true;
  }

  @Override
  public boolean stopQuickConnect() {
    _disableInAirNetwork();
    return true;
  }

  @Override
  public void startScan(boolean quickConnect) {
    mQuickConnect = quickConnect;
    mNsdHelper.startDiscover();
  }

  @Override
  public void stopScan() {
    mNsdHelper.stopDiscover();
  }

  @Override
  protected void connectionFailed() {
    System.out.println("WifiAdapter.connectionFailed");
    mConnectInAiR = false;
    if (mClient.isInSettingMode() && mSetupInAiR) {
      _connectToInAiR();
    } else {
      super.connectionFailed();
    }
  }

  @Override
  protected void connectionLost() {
    System.out.println("WifiAdapter.connectionLost");
    mConnectInAiR = false;
    super.connectionLost();
  }

  //public static void connectWifiTo(String ssid, String bssid, String capabilities, String password) {
  //  WifiConfiguration config = new WifiConfiguration();
  //  config.SSID = "\"" + ssid + "\"";
  //  config.BSSID = bssid;
  //
  //  if (capabilities.contains("PSK")) {
  //    config.preSharedKey = "\"" + password + "\"";
  //  } else if (capabilities.contains("WEP")) {
  //    config.wepKeys[0] = "\"" + password + "\"";
  //    config.wepTxKeyIndex = 0;
  //    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
  //    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
  //  } else {
  //    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
  //  }
  //
  //  WifiAdapter ins = getInstance();
  //  ins._disableInAirNetwork();
  //
  //  Log.d(TAG, "connectTo " + ssid + " " + bssid + " " + capabilities + " " + password);
  //  ins.mManager.setWifiEnabled(true);
  //
  //  int netId = ins.mManager.addNetwork(config);
  //  ins.mManager.disconnect();
  //  ins.mManager.enableNetwork(netId, false);
  //  ins.mManager.reconnect();
  //  ins.mManager.saveConfiguration();
  //  getInstance().startQuickConnect();
  //}

  @Override
  public synchronized boolean connect(Device device) {
    if (device == null) {
      return false;
    }

    if (device.parcelable != null && device.address == null) {
      mNsdHelper.resolveAndConnectDevice(device);
      return false;
    }

    if (mConnectThread != null) {
      if (mConnectThread.isAlive() && device.equals(mConnectThread.mDevice)) {
        return false;
      }
      mConnectThread.cancel();
      mConnectThread = null;
    }

    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    if (INAIR_DEVICE == device) {
      mConnectInAiR = true;
    }

    System.out.println("WifiAdapter.connect " + device);

    setState(STATE_CONNECTING);

    mConnectThread = new ConnectThread(device);
    mConnectThread.start();
    return true;
  }

  @Override
  public void stop() {
    System.out.println("WifiAdapter.stop");

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
      r.write(data);
    }
  }
  //endregion

  //region Thread
  private class ConnectThread extends Thread {
    Device mDevice;

    public ConnectThread(Device device) {
      mDevice = device;
    }

    @Override
    public void run() {
      Log.i(TAG, "BEGIN ConnectThread " + mDevice.address);
      setName("ConnectThread");
      Socket socket = null;

      boolean ok = false;
      while (!ok) {
        try {
          socket = new Socket(InetAddress.getByName(mDevice.address), INAIR_PORT);
          socket.setTcpNoDelay(true);
          ok = true;
        } catch (IOException e) {
          e.printStackTrace();
          if (mClient.isInSettingMode() && mSetupInAiR) {
            try {
              sleep(200);
            } catch (InterruptedException ignored) {
            }
          } else {
            connectionFailed();
            return;
          }
        }
      }

      synchronized (WifiAdapter.this) {
        mConnectThread = null;
        mConnectInAiR = false;
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
    private InputStream mDataIS;

    public ConnectedThread(Socket socket) {
      Log.d(TAG, "CREATE ConnectedThread");
      mmSocket = socket;

      // Get the Socket input and output streams
      try {
        mDataIS = socket.getInputStream();
      } catch (IOException e) {
        Log.e(TAG, "temp sockets not created", e);
      }
    }

    @Override
    public void run() {
      Log.i(TAG, "BEGIN ConnectedThread");
      DataInputStream is = new DataInputStream(mDataIS);
      byte[] buffer = new byte[4096];
      int length;

      // Keep listening to the InputStream while connected
      while (!isInterrupted() && mmSocket.isConnected()) {
        try {
          if (mDataIS.available() > 0) {
            //System.out.println("AVAIL " + mDataIS.available());
            length = is.readInt();
            is.readFully(buffer, 0, length);

            // Send the obtained bytes to the UI Activity
            mHandler.obtainMessage(BaseConnection.Constants.MESSAGE_READ, length, -1, Arrays.copyOf(buffer, length))
                .sendToTarget();
          }
        } catch (IOException e) {
          e.printStackTrace();
          break;
        }
      }

      Log.e(TAG, "DISCONNECTED");
      cancel();
    }

    /**
     * Write to the connected OutStream.
     *
     * @param buffer The bytes to write
     */
    public void write(byte[] buffer) {
      try {
        final OutputStream os = mmSocket.getOutputStream();
        os.write(buffer);
        os.flush();
      } catch (IOException e) {
        Log.e(TAG, "Exception during write", e);
        cancel();
      }
    }

    public void cancel() {
      try {
        interrupt();
        Log.d(TAG, "ConnectedThread.cancel");
        mmSocket.close();
        connectionLost();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }
  //endregion

  private class NSDHelper {

    private static final String TAG = "NsdHelper";
    private static final String SERVICE_TYPE = "_irpc._tcp.";

    private NsdManager mNsdManager;
    private boolean isScaning = false;
    private DiscoveryListener discoveryListener = null;

    public void registerListener(Context context) {
      mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void unregisterListener(Context context) {
      if (mNsdManager != null) {
        stopDiscover();
        mNsdManager = null;
      }
    }

    public void startDiscover() {
      if (!mManager.isWifiEnabled()) {
        mManager.setWifiEnabled(true);
      }
      if (isScaning || discoveryListener != null) {
        stopDiscover();
      }
      discoveryListener = new DiscoveryListener();
      mNsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
      isScaning = true;
    }

    public void stopDiscover() {
      if (discoveryListener != null) {
        try {
          mNsdManager.stopServiceDiscovery(discoveryListener);
        } catch (Exception e) {
          e.printStackTrace();
        }
        discoveryListener = null;
      }
      isScaning = false;
    }

    public void resolveAndConnectDevice(Device device) {
      NsdServiceInfo info = (NsdServiceInfo) device.parcelable;
      ResolveListener listener = new ResolveListener();
      listener.currentDevice = device;
      mNsdManager.resolveService(info, listener);
    }

    //region NsdManager.DiscoveryListener
    private class DiscoveryListener implements NsdManager.DiscoveryListener {

      @Override
      public void onDiscoveryStarted(String regType) {
        Log.d(TAG, "Service discovery started " + regType);
      }

      @Override
      public void onServiceFound(NsdServiceInfo service) {
        Log.d(TAG, "Service discovery success " + service);
        Device device = new Device(service.getServiceName(), null);
        device.parcelable = service;
        if (mQuickConnect) {
          mQuickConnect = false;
          stopDiscover();
          resolveAndConnectDevice(device);
        } else {
          onDeviceFound(device);
        }
      }

      @Override
      public void onServiceLost(NsdServiceInfo service) {
        Log.e(TAG, "service lost" + service);
      }

      @Override
      public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
      }

      @Override
      public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        stopDiscover();
      }

      @Override
      public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        stopDiscover();
      }
    }
    //endregion

    //region NsdManager.ResolveListener
    private class ResolveListener implements NsdManager.ResolveListener {
      Device currentDevice;

      @Override
      public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
        Log.e(TAG, "Resolve failed" + errorCode);
      }

      @Override
      public void onServiceResolved(final NsdServiceInfo serviceInfo) {
        Log.i(TAG, "Resolve Succeeded. " + serviceInfo);

        mHandler.post(new Runnable() {
          @Override
          public void run() {
            currentDevice.address = serviceInfo.getHost().getHostAddress();
            mClient.connectTo(currentDevice);
            currentDevice = null;
          }
        });
      }
    }
    //endregion
  }
}
