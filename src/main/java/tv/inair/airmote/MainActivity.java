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

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class MainActivity extends FragmentActivity {

  MainFragment fragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Application.getSocketClient().register(this);

    if (savedInstanceState == null) {
      FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
      if (fragment == null) {
        fragment = new MainFragment();
        fragment.setRetainInstance(true);
      }
      transaction.replace(R.id.fragment, fragment);
      transaction.commit();
    }
  }

  @Override
  public void onBackPressed() {
    if (!fragment.onBackPressed()) {
      super.onBackPressed();
    }
  }

  @Override
  protected void onDestroy() {
    Application.getSocketClient().unregister();
    Application.notify(this, null);
    super.onDestroy();
  }
}
