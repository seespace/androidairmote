package tv.inair.airmote.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class BTAdapter {
  private static final String TAG = "Listener";

  private static final String NAME_SECURE = "inAiRSecure";
  private static final String NAME_INSECURE = "inAiRInsecure";

  // Unique UUID for this application
  private static final String INSECURE = "B1B82063-8AE8-4C4C-8D43-ED895D423C13";
  private static final UUID MY_UUID_INSECURE = UUID.fromString(INSECURE);
  private static final UUID MY_UUID_SECURE = UUID.fromString("B978CFA5-C1EB-43F0-83EC-8B7417988D72");

  // Member fields
  public final BluetoothAdapter adapter;
  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;
  private int mState;

  private Handler mHandler;

  // Constants that indicate the current connection state
  public static final int STATE_NONE = 0;       // we're doing nothing
  public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
  public static final int STATE_CONNECTED = 3;  // now connected to a remote device

  public boolean checkIfInAiR(Parcelable[] uuids) {
    if (uuids == null || uuids.length == 0) {
      return false;
    }

    for (Parcelable uuid : uuids) {
      if (INSECURE.equalsIgnoreCase(uuid.toString())) {
        return true;
      }
    }
    return false;
  }

  private static BTAdapter instance;

  public static BTAdapter getInstance() {
    if (instance == null) {
      instance = new BTAdapter();
    }
    return instance;
  }

  private BTAdapter() {
    this.adapter = BluetoothAdapter.getDefaultAdapter();
    this.mState = STATE_NONE;
  }

  public void setHandler(Handler handler) {
    this.mHandler = handler;
  }

  public boolean connect(String address) {
    return connect(adapter.getRemoteDevice(address));
  }

  public synchronized boolean connect(BluetoothDevice device) {
    Log.d(TAG, "connect to: " + device);

    if (device == null) {
      return false;
    }

    // Cancel any thread attempting to make a connection
    if (mState == STATE_CONNECTING) {
      if (mConnectThread != null) {
        mConnectThread.cancel();
        mConnectThread = null;
      }
    }

    // Cancel any thread currently running a connection
    if (mConnectedThread != null) {
      mConnectedThread.cancel();
      mConnectedThread = null;
    }

    // Start the thread to connect with the given device
    mConnectThread = new ConnectThread(device);
    mConnectThread.start();
    setState(STATE_CONNECTING);
    return true;
  }

  public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
    Log.d(TAG, "connected, Socket Type:" + socketType);

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
    mConnectedThread = new ConnectedThread(socket, socketType);
    mConnectedThread.start();

    // Send the name of the connected device back to the UI Activity
    Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
    Bundle bundle = new Bundle();
    bundle.putString(Constants.DEVICE_NAME, device.getName());
    msg.setData(bundle);
    mHandler.sendMessage(msg);

    setState(STATE_CONNECTED);
  }

  public synchronized void stop() {
    Log.d(TAG, "stop");

    if (mConnectThread != null) {
      mConnectThread.interrupt();
    }

    if (mConnectedThread != null) {
      mConnectedThread.interrupt();
    }

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

  public void write(byte[] out) {
    // Create temporary object
    ConnectedThread r;
    // Synchronize a copy of the ConnectedThread
    synchronized (this) {
      if (mState != STATE_CONNECTED) {
        return;
      }
      r = mConnectedThread;
    }
    // Perform the write unsynchronized
    r.write(out);
  }

  public boolean isConnected() {
    return mState == STATE_CONNECTED;
  }

  /**
   * Indicate that the connection attempt failed and notify the UI Activity.
   */
  private void connectionFailed() {
    setState(STATE_NONE);
  }

  private void connectionLost() {
    setState(STATE_NONE);
  }

  private synchronized void setState(int state) {
    mState = state;
    mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
  }

  /**
   * This thread runs while attempting to make an outgoing connection
   * with a device. It runs straight through; the connection either
   * succeeds or fails.
   */
  private class ConnectThread extends Thread {
    private final BluetoothSocket mmSocket;
    private final BluetoothDevice mmDevice;
    private String mSocketType;

    public ConnectThread(BluetoothDevice device) {
      mmDevice = device;
      BluetoothSocket tmp = null;
      mSocketType = "Secure";

      // Get a BluetoothSocket for a connection with the
      // given BluetoothDevice
      try {
        tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_INSECURE);
        //        tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
      } catch (IOException e) {
        Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
      }
      mmSocket = tmp;
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
      setName("ConnectThread" + mSocketType);

      // Always cancel discovery because it will slow down a connection
      adapter.cancelDiscovery();

      // Make a connection to the BluetoothSocket
      try {
        // This is a blocking call and will only return on a
        // successful connection or an exception
        mmSocket.connect();
      } catch (IOException e) {
        // Close the socket
        try {
          mmSocket.close();
        } catch (IOException e2) {
          Log.e(TAG, "unable to close() " + mSocketType +
                     " socket during connection failure", e2);
        }
        connectionFailed();
        return;
      }

      // Reset the ConnectThread because we're done
      synchronized (BTAdapter.this) {
        mConnectThread = null;
      }

      // Start the connected thread
      connected(mmSocket, mmDevice, mSocketType);
    }

    public void cancel() {
      while (!isInterrupted()) {}
      try {
        System.out.println("ConnectThread.cancel");
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
      }
    }
  }

  /**
   * This thread runs during a connection with a remote device.
   * It handles all incoming and outgoing transmissions.
   */
  private class ConnectedThread extends Thread {
    private final BluetoothSocket mmSocket;
    //    private final InputStream mmInStream;
    //    private final OutputStream mmOutStream;
    private final DataInputStream mDataIS;
    private final DataOutputStream mDataOS;

    public ConnectedThread(BluetoothSocket socket, String socketType) {
      Log.d(TAG, "create ConnectedThread: " + socketType);
      mmSocket = socket;
      InputStream tmpIn = null;
      OutputStream tmpOut = null;

      // Get the BluetoothSocket input and output streams
      try {
        tmpIn = socket.getInputStream();
        tmpOut = socket.getOutputStream();
      } catch (IOException e) {
        Log.e(TAG, "temp sockets not created", e);
      }

      mDataIS = new DataInputStream(tmpIn);
      mDataOS = new DataOutputStream(tmpOut);
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectedThread");
      byte[] buffer = new byte[4096];
      int length;

      // Keep listening to the InputStream while connected
      while (!isInterrupted()) {
        try {
          if (mDataIS.available() > 0) {
            length = mDataIS.readInt();
            mDataIS.readFully(buffer, 0, length);

            // Send the obtained bytes to the UI Activity
            mHandler.obtainMessage(Constants.MESSAGE_READ, length, -1, Arrays.copyOf(buffer, length)).sendToTarget();
          }
        } catch (IOException e) {
          Log.e(TAG, "disconnected", e);
          connectionLost();
          break;
        }

      }
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
      }
    }

    public void cancel() {
      while (!isInterrupted()) {}
      try {
        System.out.println("ConnectedThread.cancel");
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }
}
