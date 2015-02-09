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
public class AiRmote extends Application {

  private static SocketClient mSocketClient;
  public static SocketClient getSocketClient() {
    return mSocketClient;
  }

  private static final String TEMP = "#temp";
  private static SharedPreferences mTempPreferences;
  public static SharedPreferences getTempPreferences() {
    return mTempPreferences;
  }

  @Override
  public void onCreate() {
    mSocketClient = new SocketClient();
    // Clear temporary preferences
    mTempPreferences = getSharedPreferences(TEMP, Context.MODE_PRIVATE);
    getTempPreferences().edit().clear().commit();
  }
}
