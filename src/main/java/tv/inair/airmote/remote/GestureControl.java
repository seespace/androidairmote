package tv.inair.airmote.remote;

import android.app.Activity;
import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import tv.inair.airmote.Application;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2014 SeeSpace.co. All rights reserved.</p>
 */
public class GestureControl implements View.OnTouchListener, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
  private static final String DEBUG_TAG = "Gestures";

  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  private final GestureDetectorCompat mDetector;
  private final View mElement;

  private boolean mHolding;
  private boolean mPanning;
  private MotionEvent mLastMotion;
  private long mLastTime;

  final DisplayMetrics metrics = new DisplayMetrics();

  public GestureControl(Context context, View element) {
    ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(metrics);

    mDetector = new GestureDetectorCompat(context, this);
    // Set the gesture detector as the double tap
    // listener.
    mDetector.setOnDoubleTapListener(this);

    mElement = element;
    element.setOnTouchListener(this);
  }

  public interface Listener {
    void onEvent();
  }

  private Listener mListener;

  private void onEvent() {
    if (mListener != null) {
      mListener.onEvent();
    }
  }

  public void setListener(Listener listener) {
    mListener = listener;
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    onEvent();
    Integer phase = null;
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
//        Log.d(DEBUG_TAG, "touch: BEGIN");
        phase = Proto.BEGAN;
        break;
      case MotionEvent.ACTION_MOVE:
//        Log.d(DEBUG_TAG, "touch: MOVE " + event.getX() + " " + mElement.getWidth() + " " + event.getY() + " " + mElement.getHeight());
        phase = Proto.MOVED;
        break;
      case MotionEvent.ACTION_UP:
//        Log.d(DEBUG_TAG, "touch: END");
        phase = Proto.ENDED;
        break;
      case MotionEvent.ACTION_CANCEL:
        phase = Proto.CANCELLED;
        break;
    }
    if (phase != null) {
      Application.getSocketClient().sendEvent(Helper.newTouchEvent(
          Helper.now(),
          event.getX(), event.getY(),
          mElement.getWidth(), mElement.getHeight(),
          phase
      ));
    }

    mDetector.onTouchEvent(event);

    Proto.Event e = null;
    long now = Helper.now();
    if (mHolding) {
      if (mPanning) {
//        Log.d(DEBUG_TAG, "pan: CANCELED");
        e = Helper.newPanEvent(
            now,
            event.getX(), event.getY(),
            mElement.getWidth(), mElement.getHeight(),
            Proto.GestureEvent.CANCELLED,
            0, 0,
            0, 0
        );
        mLastMotion = null;
        mPanning = false;
      }
      Integer state = null;
      long duration = 0;
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_MOVE:
          state = Proto.GestureEvent.CHANGED;
//          Log.d(DEBUG_TAG, "holding: CHANGED");
          break;
        case MotionEvent.ACTION_UP:
          state = Proto.GestureEvent.ENDED;
          duration = now - mLastTime;
          mLastTime = 0;
          mHolding = false;
//          Log.d(DEBUG_TAG, "holding: ENDED");
          break;
      }
      if (state != null) {
        e = Helper.newLongPressEvent(
            now,
            event.getX(), event.getY(),
            mElement.getWidth(), mElement.getHeight(),
            state,
            duration
        );
      }
    } else if (mPanning && event.getActionMasked() == MotionEvent.ACTION_UP) {
//      Log.d(DEBUG_TAG, "pan: ENDED");
      e = Helper.newPanEvent(
          now,
          event.getX(), event.getY(),
          mElement.getWidth(), mElement.getHeight(),
          Proto.GestureEvent.ENDED,
          event.getX() - mLastMotion.getX(), event.getY() - mLastMotion.getY(),
          0, 0
      );
      mLastMotion = null;
      mPanning = false;
    }

    if (e != null) {
      Application.getSocketClient().sendEvent(e);
    }
    return true;
  }

  @Override
  public boolean onDown(MotionEvent event) {
    if (!mHolding && !mPanning) {
//      Log.d(DEBUG_TAG, "pan: BEGAN");
      mPanning = true;
      mLastMotion = event;
      Application.getSocketClient().sendEvent(
          Helper.newPanEvent(
              Helper.now(),
              event.getX(),
              event.getY(),
              mElement.getWidth(),
              mElement.getHeight(),
              Proto.GestureEvent.BEGAN,
              0,
              0,
              0,
              0
          )
      );
    }
    return true;
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    if (mHolding) {
      return false;
    }

    float absVeloX = Math.abs(velocityX / metrics.xdpi * 25.4f);
    float absVeloY = Math.abs(velocityY / metrics.ydpi * 25.4f);

    if (absVeloX < SWIPE_VELOCITY_THRESHOLD && absVeloY < SWIPE_VELOCITY_THRESHOLD) {
      return true;
    }

    int direction;
    float dY = e2.getY() - e1.getY();
    float dX = e2.getX() - e1.getX();

    if (Math.abs(dX) > Math.abs(dY)) {
      direction = dX > 0
                  ? Proto.GestureEvent.RIGHT
                  : Proto.GestureEvent.LEFT;
    } else {
      direction = dY > 0
                  ? Proto.GestureEvent.DOWN
                  : Proto.GestureEvent.UP;
    }

//    Log.d(DEBUG_TAG, "onFling: " + direction);

    Application.getSocketClient().sendEvent(
        Helper.newSwipeEvent(
            Helper.now(),
            e2.getX(),
            e2.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.ENDED,
            direction
        )
    );
    return true;
  }

  @Override
  public void onLongPress(MotionEvent event) {
    if (mHolding) {
      return;
    }

    mHolding = true;
    mLastTime = Helper.now();
    Application.getSocketClient().sendEvent(
        Helper.newLongPressEvent(
            mLastTime,
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.BEGAN,
            0
        )
    );
//    Log.d(DEBUG_TAG, "holding: BEGAN ");
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    if (mHolding) {
      return false;
    }

//    Log.d(DEBUG_TAG, "onPan: CHANGED");
    mLastMotion = e2;

    float velocityX, velocityY;
    velocityX = 1000 * (e2.getX() - e1.getX()) / (e2.getEventTime() - e1.getEventTime());
    velocityY = 1000 * (e2.getY() - e1.getY()) / (e2.getEventTime() - e1.getEventTime());

    Application.getSocketClient().sendEvent(
        Helper.newPanEvent(
            Helper.now(),
            e2.getX(), e2.getY(),
            mElement.getWidth(), mElement.getHeight(),
            Proto.GestureEvent.CHANGED,
            e2.getX() - e1.getX(), e2.getY() - e1.getY(),
            velocityX, velocityY
        )
    );

    return true;
  }

  @Override
  public void onShowPress(MotionEvent event) {
//    Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
  }

  @Override
  public boolean onSingleTapUp(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onDoubleTap(MotionEvent event) {
//    Log.d(DEBUG_TAG, "onDoubleTap: ");
    Application.getSocketClient().sendEvent(
        Helper.newTapEvent(
            Helper.now(),
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.ENDED,
            2
        )
    );
    return true;
  }

  @Override
  public boolean onDoubleTapEvent(MotionEvent event) {
    return true;
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent event) {
//    Log.d(DEBUG_TAG, "onSingleTapEvent: ");
    Application.getSocketClient().sendEvent(
        Helper.newTapEvent(
            Helper.now(),
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.ENDED,
            1
        )
    );
    return true;
  }
}
