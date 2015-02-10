package tv.inair.airmote.connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

  // Unique UUID for this application
  private static final UUID MY_UUID_SECURE = UUID.fromString("B1B82063-8AE8-4C4C-8D43-ED895D423C13");

  // Member fields
  private final BluetoothAdapter mAdapter;
  private ConnectThread mConnectThread;
  private ConnectedThread mConnectedThread;
  private int mState;

  private Handler mHandler;

  // Constants that indicate the current connection state
  public static final int STATE_NONE = 0;       // we're doing nothing
  public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
  public static final int STATE_CONNECTED = 3;  // now connected to a remote device

  public BTAdapter(Handler handler) {
    this.mAdapter = BluetoothAdapter.getDefaultAdapter();
    this.mHandler = handler;
    this.mState = STATE_NONE;
  }

  public void connect(String address) {
    connect(mAdapter.getRemoteDevice(address));
  }

  public synchronized void connect(BluetoothDevice device) {
    Log.d(TAG, "connect to: " + device);

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
      if (mState != STATE_CONNECTED) return;
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
    // Send a failure message back to the Activity
//    Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//    Bundle bundle = new Bundle();
//    bundle.putString(Constants.TOAST, "Unable to connect device");
//    msg.setData(bundle);
//    mHandler.sendMessage(msg);
  }

  private void connectionLost() {
    // Send a failure message back to the Activity
//    Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
//    Bundle bundle = new Bundle();
//    bundle.putString(Constants.TOAST, "Device connection was lost");
//    msg.setData(bundle);
//    mHandler.sendMessage(msg);
  }

  private synchronized void setState(int state) {
    mState = state;
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
        tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
      } catch (IOException e) {
        Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
      }
      mmSocket = tmp;
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
      setName("ConnectThread" + mSocketType);

      // Always cancel discovery because it will slow down a connection
      mAdapter.cancelDiscovery();

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
      try {
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
    private final InputStream mmInStream;
    private final OutputStream mmOutStream;

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

      mmInStream = tmpIn;
      mmOutStream = tmpOut;
    }

    public void run() {
      Log.i(TAG, "BEGIN mConnectedThread");
      byte[] buffer = new byte[1024];
      int bytes;

      // Keep listening to the InputStream while connected
      while (true) {
        try {
          if (mmInStream.available() > 0) {
            System.out.println("Avail: " + mmInStream.available());
            // Read from the InputStream
            bytes = mmInStream.read(buffer);

            System.out.println("RECEIVE: " + bytes);

            // Send the obtained bytes to the UI Activity
            mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
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
        mmOutStream.write(buffer);

        // Share the sent message back to the UI Activity
        mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
      } catch (IOException e) {
        Log.e(TAG, "Exception during write", e);
      }
    }

    public void cancel() {
      try {
        mmSocket.close();
      } catch (IOException e) {
        Log.e(TAG, "close() of connect socket failed", e);
      }
    }
  }
}
