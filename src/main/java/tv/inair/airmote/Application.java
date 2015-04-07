package tv.inair.airmote;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import java.lang.ref.WeakReference;

import io.fabric.sdk.android.Fabric;
import tv.inair.airmote.connection.SocketClient;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2014 SeeSpace.co. All rights reserved.</p>
 */
public class Application extends android.app.Application {

  public static final String FIRST_TIME_KEY = "#first_time";

  private static SocketClient mSocketClient;

  public static SocketClient getSocketClient() {
    return mSocketClient;
  }

  private static final String TEMP = "#temp";
  private static SharedPreferences mTempPreferences;

  public static SharedPreferences getTempPreferences() {
    return mTempPreferences;
  }

  private static final String SETTINGS = "#settings";
  private static SharedPreferences mSettingsPreferences;

  public static SharedPreferences getSettingsPreferences() {
    return mSettingsPreferences;
  }

  public static final int ERROR_COLOR = Color.parseColor("#ffff4444");
  public static final int NORMAL_COLOR = Color.parseColor("#ffe6e6e6");
  public static final int SUCCESS_COLOR = Color.parseColor("#ff33b5e5");

  public enum Status {
    ERROR,
    NORMAL,
    SUCCESS
  }

  private static String mCurrentMessage = "Disconnected";
  private static Status mCurrentType = Status.ERROR;

  private static WeakReference<TextView> mStatus;
  public static void setStatusView(TextView status) {
    if (status != null) {
      mStatus = new WeakReference<>(status);
    }

    notify(mCurrentMessage, mCurrentType);
  }

  public static void notify(String message, Status type) {
    mCurrentType = type;
    mCurrentMessage = message;

    if (mStatus != null && mStatus.get() != null) {
      TextView view = mStatus.get();
      view.setText(mCurrentMessage);
      view.setTypeface(view.getTypeface(), Typeface.NORMAL);
      switch (mCurrentType) {
        case ERROR:
          view.setBackgroundColor(ERROR_COLOR);
          break;

        case NORMAL:
          view.setTypeface(view.getTypeface(), Typeface.BOLD);
          view.setBackgroundColor(NORMAL_COLOR);
          break;

        case SUCCESS:
          view.setBackgroundColor(SUCCESS_COLOR);
          break;
      }
    }
  }

  @Override
  public void onCreate() {
    Fabric.with(this, new Crashlytics());
    mSocketClient = new SocketClient();
    // Clear temporary preferences
    mTempPreferences = getSharedPreferences(TEMP, Context.MODE_PRIVATE);
    getTempPreferences().edit().clear().commit();

    mSettingsPreferences = getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
  }
}
