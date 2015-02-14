package tv.inair.airmote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class WifiConnectActivity extends Activity {

  public static final String EXTRA_SSID = "extra_ssid";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setResult(Activity.RESULT_CANCELED);

    Intent i = getIntent();
    setTitle("Connect to " + i.getStringExtra(EXTRA_SSID));

    setContentView(R.layout.activity_connect);
  }
}
