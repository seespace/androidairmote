package tv.inair.airmote;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;

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

  private static final String TAG = "inAiR";

  public static void notify(Context context, String message) {
    NotificationManager manager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
    if (message == null) {
      manager.cancel(0);
    } else {
      NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(R.drawable.ic_launcher)
          .setLocalOnly(true)
          .setPriority(Notification.PRIORITY_MAX)
          .setContentTitle(TAG)
          .setContentText(message);
      manager.notify(0, builder.build());
    }
  }

  @Override
  public void onCreate() {
    mSocketClient = new SocketClient();
    // Clear temporary preferences
    mTempPreferences = getSharedPreferences(TEMP, Context.MODE_PRIVATE);
    getTempPreferences().edit().clear().commit();

    mSettingsPreferences = getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
  }
}
