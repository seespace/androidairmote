/**
 *
 * The MIT License
 *
 * Copyright (c) 2014, SeeSpace. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package tv.inair.airmote;

import android.app.ActionBar;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends FragmentActivity {

  public static final int REQUEST_WIFI_SETUP = 1;
  public static final int REQUEST_WIFI_CONNECT = 2;
  public static final int REQUEST_SCAN_INAIR = 10;
  public static final int REQUEST_TEXT_INPUT = 20;

  private MainFragment mMainFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Application.setStatusView(((TextView) findViewById(R.id.status)));

    if (savedInstanceState == null) {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      if (mMainFragment == null) {
        mMainFragment = new MainFragment();
        mMainFragment.setRetainInstance(true);
      }
      transaction.add(R.id.fragment, mMainFragment);
      transaction.commit();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    View decorView = getWindow().getDecorView();
    // Hide the status bar.
    int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
    decorView.setSystemUiVisibility(uiOptions);
    // Remember that you should never show the action bar if the
    // status bar is hidden, so hide that too if necessary.
    ActionBar actionBar = getActionBar();
    if (actionBar != null) {
      actionBar.hide();
    }
  }

  @Override
  public void onBackPressed() {
    if (mMainFragment != null && !mMainFragment.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onDestroy() {
    Application.getSocketClient().unregister();
    super.onDestroy();
  }
}
