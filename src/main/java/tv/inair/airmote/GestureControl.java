package tv.inair.airmote;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2014 SeeSpace.co. All rights reserved.</p>
 */
class GestureControl implements View.OnTouchListener, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {
  private static final String DEBUG_TAG = "Gestures";

  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  private final GestureDetectorCompat mDetector;
  private final View mElement;

  private boolean mHolding;
  private boolean mPanning;
  private MotionEvent mLastMotion;
  private long mLastTime;

  public GestureControl(Context context, View element) {
    mDetector = new GestureDetectorCompat(context, this);
    // Set the gesture detector as the double tap
    // listener.
    mDetector.setOnDoubleTapListener(this);

    mElement = element;
    element.setOnTouchListener(this);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    mDetector.onTouchEvent(event);

    if (mHolding) {
      long now = Helper.now();
      if (mPanning) {
        Log.d(DEBUG_TAG, "pan: CANCELED");
        AiRMote.getSocketClient().sendEvent(
            Helper.newPanEvent(
                now,
                event.getX(),
                event.getY(),
                mElement.getWidth(),
                mElement.getHeight(),
                Proto.GestureEvent.State.CANCELLED,
                0,
                0,
                0,
                0
            )
        );
        mLastMotion = null;
        mPanning = false;
      }
      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_MOVE:
          AiRMote.getSocketClient().sendEvent(
              Helper.newLongPressEvent(
                  now,
                  event.getX(),
                  event.getY(),
                  mElement.getWidth(),
                  mElement.getHeight(),
                  Proto.GestureEvent.State.CHANGED,
                  0
              )
          );
          Log.d(DEBUG_TAG, "holding: CHANGED");
          break;
        case MotionEvent.ACTION_UP:
          AiRMote.getSocketClient().sendEvent(
              Helper.newLongPressEvent(
                  now,
                  event.getX(),
                  event.getY(),
                  mElement.getWidth(),
                  mElement.getHeight(),
                  Proto.GestureEvent.State.ENDED,
                  now - mLastTime
              )
          );
          mLastTime = 0;
          mHolding = false;
          Log.d(DEBUG_TAG, "holding: ENDED");
          break;
      }
    } else if (mPanning && event.getActionMasked() == MotionEvent.ACTION_UP) {
      Log.d(DEBUG_TAG, "pan: ENDED");
      AiRMote.getSocketClient().sendEvent(
          Helper.newPanEvent(
              Helper.now(),
              event.getX(),
              event.getY(),
              mElement.getWidth(),
              mElement.getHeight(),
              Proto.GestureEvent.State.ENDED,
              event.getX() - mLastMotion.getX(),
              event.getY() - mLastMotion.getY(),
              0,
              0
          )
      );
      mLastMotion = null;
      mPanning = false;
    }
    return true;
  }

  @Override
  public boolean onDown(MotionEvent event) {
    if (!mHolding && !mPanning) {
      Log.d(DEBUG_TAG, "pan: BEGAN");
      mPanning = true;
      mLastMotion = event;
      AiRMote.getSocketClient().sendEvent(
          Helper.newPanEvent(
              Helper.now(),
              event.getX(),
              event.getY(),
              mElement.getWidth(),
              mElement.getHeight(),
              Proto.GestureEvent.State.BEGAN,
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

    float absVeloX = Math.abs(velocityX);
    float absVeloY = Math.abs(velocityY);

    if (absVeloX < SWIPE_VELOCITY_THRESHOLD && absVeloY < SWIPE_VELOCITY_THRESHOLD) {
      return true;
    }

    Proto.GestureEvent.SwipeDirection direction;
    float dY = e2.getY() - e1.getY();
    float dX = e2.getX() - e1.getX();

    if (Math.abs(dX) > Math.abs(dY)) {
      direction = dX > 0
                  ? Proto.GestureEvent.SwipeDirection.RIGHT
                  : Proto.GestureEvent.SwipeDirection.LEFT;
    } else {
      direction = dY > 0
                  ? Proto.GestureEvent.SwipeDirection.DOWN
                  : Proto.GestureEvent.SwipeDirection.UP;
    }

//    Log.d(DEBUG_TAG, "onFling: " + direction);

    AiRMote.getSocketClient().sendEvent(
        Helper.newSwipeEvent(
            Helper.now(),
            e2.getX(),
            e2.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.State.ENDED,
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
    AiRMote.getSocketClient().sendEvent(
        Helper.newLongPressEvent(
            mLastTime,
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.State.BEGAN,
            0
        )
    );
    Log.d(DEBUG_TAG, "holding: BEGAN ");
  }

  @Override
  public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
    if (mHolding) {
      return false;
    }

    Log.d(DEBUG_TAG, "onPan: CHANGED");
    mLastMotion = e2;

    AiRMote.getSocketClient().sendEvent(
        Helper.newPanEvent(
            Helper.now(),
            e2.getX(),
            e2.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.State.CHANGED,
            distanceX,
            distanceY,
            0,
            0
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
    AiRMote.getSocketClient().sendEvent(
        Helper.newTapEvent(
            Helper.now(),
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.State.ENDED,
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
    AiRMote.getSocketClient().sendEvent(
        Helper.newTapEvent(
            Helper.now(),
            event.getX(),
            event.getY(),
            mElement.getWidth(),
            mElement.getHeight(),
            Proto.GestureEvent.State.ENDED,
            1
        )
    );
    return true;
  }
}
