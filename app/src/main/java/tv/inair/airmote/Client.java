package tv.inair.airmote;

import android.content.Context;
import android.view.MotionEvent;
import inair.eventcenter.proto.Proto;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Copyright (c) 2014 SeeSpace.co. All rights reserved.
 */

class Client {

  private Socket mSocket;

  private Context mContext;

  private DataOutputStream mWriter;

  private static final int SWIPE_THRESHOLD = 100;
  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  Client(Context context, Socket socket) {
    mContext = context;
    mSocket = socket;

    try {
      mWriter = new DataOutputStream(mSocket.getOutputStream());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  boolean isConnected() {
    return mSocket != null && mSocket.isConnected();
  }

  void dispatchEvent(Proto.Event event) {
    byte[] data = ProtoHelper.dataFromEvent(event);
    try {
      mWriter.write(data);
      mWriter.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  void registerDevice() {
    Proto.Event event = ProtoHelper.newDeviceEvent(mContext, ProtoHelper.now(), Proto.DeviceEvent.Type.REGISTER);
    dispatchEvent(event);
  }

  void sendTouchEvent(MotionEvent e) {
    Proto.Phase phase;
    switch (e.getActionMasked()) {
      case (MotionEvent.ACTION_DOWN) :
        phase = Proto.Phase.BEGAN;
        break;
      case (MotionEvent.ACTION_MOVE) :
        phase = Proto.Phase.MOVED;
        break;
      case (MotionEvent.ACTION_UP) :
        phase = Proto.Phase.ENDED;
        break;
      case (MotionEvent.ACTION_CANCEL) :
        phase = Proto.Phase.CANCELLED;
        break;
      case (MotionEvent.ACTION_OUTSIDE) :
        phase = Proto.Phase.ENDED;
        break;
      default :
        phase = Proto.Phase.ENDED;
    }

    Proto.Event event = ProtoHelper.newTouchEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, phase);

    dispatchEvent(event);
  }

  void sendLongPressEvent(MotionEvent e) {
    Proto.Event event = ProtoHelper.newLongPressEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, e.getDownTime());
    dispatchEvent(event);

//    System.out.println("GESTURE: Long Press");
  }

  void sendSingleTapEvent(MotionEvent e) {
    Proto.Event event = ProtoHelper.newTapEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, 1);
    dispatchEvent(event);

//    System.out.println("GESTURE: Tap");
  }

  void sendDoubleTapEvent(MotionEvent e) {
    Proto.Event event = ProtoHelper.newTapEvent(e.getEventTime(), e.getX(), e.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, 2);
    dispatchEvent(event);

//    System.out.println("GESTURE: Double Tap");
  }

  void sendSwipeEvent(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    Proto.GestureEvent.SwipeDirection direction = Proto.GestureEvent.SwipeDirection.DOWN;
    float diffY = e2.getY() - e1.getY();
    float diffX = e2.getX() - e1.getX();

    if (Math.abs(diffX) > Math.abs(diffY)) {
      if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffX > 0) {
          direction = Proto.GestureEvent.SwipeDirection.RIGHT;
        } else {
          direction = Proto.GestureEvent.SwipeDirection.LEFT;
        }
      }
    } else {
      if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffY > 0) {
          direction = Proto.GestureEvent.SwipeDirection.DOWN;
        } else {
          direction = Proto.GestureEvent.SwipeDirection.UP;
        }
      }
    }

    Proto.Event event = ProtoHelper.newSwipeEvent(e2.getEventTime(), e2.getX(), e2.getY(), ProtoHelper.screenSize(mContext).x, ProtoHelper.screenSize(mContext).y, Proto.GestureEvent.State.ENDED, direction);
    dispatchEvent(event);

//    System.out.println("GESTURE: Swipe " + direction.toString() + " " + velocityX + " " + velocityY);
  }
}
