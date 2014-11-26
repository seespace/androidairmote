package tv.inair.airmote;

import android.app.Application;

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
  private static SocketClient socketClient;

  public static SocketClient getSocketClient() {
    if (socketClient == null) {
      socketClient = new SocketClient();
    }
    return socketClient;
  }
}
