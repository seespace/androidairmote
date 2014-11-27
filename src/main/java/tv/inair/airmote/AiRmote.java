package tv.inair.airmote;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2014 SeeSpace.co. All rights reserved.</p>
 */
public class AiRMote extends Application {
  public static final String TEMP = "#temp";
  private static SocketClient socketClient;

  public static SocketClient getSocketClient() {
    if (socketClient == null) {
      socketClient = new SocketClient();
    }
    return socketClient;
  }

  private static SharedPreferences tempPreferences;
  public static SharedPreferences getTempPreferences() {
    return tempPreferences;
  }

  @Override
  public void onCreate() {
    // Clear temporary preferences
    tempPreferences = getSharedPreferences(TEMP, Context.MODE_PRIVATE);
    getTempPreferences().edit().clear().commit();
  }
}
