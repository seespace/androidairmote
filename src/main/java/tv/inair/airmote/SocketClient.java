package tv.inair.airmote;

import android.os.AsyncTask;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;

public class SocketClient {
  private static final int SERVER_PORT = 8989;
  private static final String HOST_NAME_KEY = "#hostname";

  private Socket mSocket;
  private OnEventReceived mEventReceived;
  private OnSocketStateChanged mStateChanged;

  private ConnectTask mConnectTask;
  private DataOutputStream mWriter;
  private DataInputStream mReader;

  public void setOnEventReceived(OnEventReceived listener) {
    mEventReceived = listener;
  }

  public void setOnSocketStateChanged(OnSocketStateChanged listener) {
    mStateChanged = listener;
  }

  public boolean isConnected() {
    return mSocket != null && mSocket.isConnected();
  }

  private void notifyDisconnect(String message) {
    disconnect();
    onStateChanged(false, message);
  }

  public void reconnectToLastHost() {
    String lastHost = "";

    System.out.println("SocketClient.reconnectToLastHost " + lastHost + " " + AiRMote.getTempPreferences().contains(HOST_NAME_KEY));

    if (AiRMote.getTempPreferences().contains(HOST_NAME_KEY)) {
      lastHost = AiRMote.getTempPreferences().getString(HOST_NAME_KEY, "");
    }
    if (lastHost.isEmpty()) {
      return;
    }
    connectTo(lastHost);
  }

  public void connectTo(String hostName) {
    if (isConnected() || (mConnectTask != null && mConnectTask.getStatus() == AsyncTask.Status.RUNNING)) {
      return;
    }
    mConnectTask = new ConnectTask();
    mConnectTask.execute(hostName);
  }

  public void disconnect() {
    if (!isConnected()) {
      return;
    }
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
    System.out.println("SocketClient.disconnect");
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
    if (mEventReceived != null) {
      mEventReceived.onEventReceived(e);
    }
  }

  private void onStateChanged(boolean connect, String message) {
    if (mStateChanged != null) {
      mStateChanged.onStateChanged(connect, message);
    }
  }

  private class Data {
    Proto.Event event;
    boolean connected;
    String message;

    private Data(Proto.Event event, boolean connected, String message) {
      this.event = event;
      this.connected = connected;
      this.message = message;
    }
  }

  // Connectivity
  private class ConnectTask extends AsyncTask<String, Data, Void> {

    @Override
    protected Void doInBackground(String... params) {
      try {
        InetAddress serverAddress = InetAddress.getByName(params[0]);
        mSocket = new Socket(serverAddress, SERVER_PORT);
        mSocket.setTcpNoDelay(true);

        publishProgress(new Data(null, true, params[0]));

        mWriter = new DataOutputStream(mSocket.getOutputStream());
        mReader = new DataInputStream(mSocket.getInputStream());

        while (!isCancelled()) {
          Thread.sleep(10);
        }

      } catch (Exception e) {
        publishProgress(new Data(null, false, e.getMessage()));
        e.printStackTrace();
      }

      return null;
    }

    @Override
    protected void onCancelled(Void aVoid) {
      System.out.println("ConnectTask.onCancelled");
    }

    @Override
    protected void onProgressUpdate(Data... values) {
      Data data = values[0];
      System.out.println("ConnectTask.onProgressUpdate " + data.connected + " " + data.message);
      if (data.event != null) {
        onEventReceived(data.event);
      } else {
        if (data.connected) {
          // store current hostname
          AiRMote.getTempPreferences().edit().putString(HOST_NAME_KEY, data.message).commit();
          onStateChanged(true, null);
        } else {
          AiRMote.getTempPreferences().edit().remove(HOST_NAME_KEY).commit();
          notifyDisconnect(data.message);
        }
      }
    }
  }

//  private Socket mSocket;
//  private Context mContext;
//
//  private DataOutputStream mWriter;
//
//  private ClientDelegate mDelegate;
//
//  private static final int SWIPE_THRESHOLD = 100;
//  private static final int SWIPE_VELOCITY_THRESHOLD = 100;
//
//  SocketClient(Context context, Socket socket) {
//    mContext = context;
//    mSocket = socket;
//
//    try {
//      mWriter = new DataOutputStream(mSocket.getOutputStream());
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//  }
//
//  public ClientDelegate getDelegate() {
//    return mDelegate;
//  }
//
//  public void setDelegate(ClientDelegate delegate) {
//    mDelegate = delegate;
//  }
//
//  boolean isConnected() {
//    return mSocket != null && mSocket.isConnected();
//  }
//
//  void dispatchEvent(Proto.Event event) {
//    byte[] data = Helper.dataFromEvent(event);
//    try {
//      mWriter.write(data);
//      mWriter.flush();
//    } catch (IOException e) {
//      e.printStackTrace();
//      if (mDelegate != null) {
//        mDelegate.onExeptionRaised(e);
//      }
//    }
//  }
//
//  void registerDevice() {
//    Proto.Event event = Helper.newDeviceEvent(mContext, Helper.now(), Proto.DeviceEvent.Type.REGISTER);
//    dispatchEvent(event);
//  }
//
//  void sendTouchEvent(MotionEvent e) {
//    Proto.Phase phase;
//    switch (e.getActionMasked()) {
//      case (MotionEvent.ACTION_DOWN) :
//        phase = Proto.Phase.BEGAN;
//        break;
//      case (MotionEvent.ACTION_MOVE) :
//        phase = Proto.Phase.MOVED;
//        break;
//      case (MotionEvent.ACTION_UP) :
//        phase = Proto.Phase.ENDED;
//        break;
//      case (MotionEvent.ACTION_CANCEL) :
//        phase = Proto.Phase.CANCELLED;
//        break;
//      case (MotionEvent.ACTION_OUTSIDE) :
//        phase = Proto.Phase.ENDED;
//        break;
//      default :
//        phase = Proto.Phase.ENDED;
//    }
//
//    Proto.Event event = Helper.newTouchEvent(e.getEventTime(), e.getX(), e.getY(), Helper.screenSize(mContext).x, Helper.screenSize(mContext).y, phase);
//
//    dispatchEvent(event);
//  }
//
//  void sendLongPressEvent(MotionEvent e) {
//    Proto.Event event = ProtoHelper.newLongPressEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, e.getDownTime());
//    dispatchEvent(event);
//
////    System.out.println("GESTURE: Long Press");
//  }
//
//  void sendSingleTapEvent(MotionEvent e) {
//    Proto.Event event = ProtoHelper.newTapEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, 1);
//    dispatchEvent(event);
//
////    System.out.println("GESTURE: Tap");
//  }
//
//  void sendDoubleTapEvent(MotionEvent e) {
//    Proto.Event event = ProtoHelper.newTapEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, 2);
//    dispatchEvent(event);
//
////    System.out.println("GESTURE: Double Tap");
//  }
//
//  void sendSwipeEvent(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//    Proto.GestureEvent.SwipeDirection direction = Proto.GestureEvent.SwipeDirection.DOWN;
//    float diffY = e2.getY() - e1.getY();
//    float diffX = e2.getX() - e1.getX();
//
//    if (Math.abs(diffX) > Math.abs(diffY)) {
//      if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
//        if (diffX > 0) {
//          direction = Proto.GestureEvent.SwipeDirection.RIGHT;
//        } else {
//          direction = Proto.GestureEvent.SwipeDirection.LEFT;
//        }
//      }
//    } else {
//      if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
//        if (diffY > 0) {
//          direction = Proto.GestureEvent.SwipeDirection.DOWN;
//        } else {
//          direction = Proto.GestureEvent.SwipeDirection.UP;
//        }
//      }
//    }
//
//    Proto.Event event = ProtoHelper.newSwipeEvent(e2.getEventTime(), e2.getX(), e2.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, direction);
//    dispatchEvent(event);

//    System.out.println("GESTURE: Swipe " + direction.toString() + " " + velocityX + " " + velocityY);
//  }
}
