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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import inair.eventcenter.proto.Proto;

public class MainActivity extends Activity implements OnEventReceived, OnSocketStateChanged {

  private GestureControl mGestureControl;

  private NsdHelper mNsdHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mNsdHelper = new NsdHelper(this);

    mGestureControl = new GestureControl(this, findViewById(R.id.rootView));

    AiRmote.getSocketClient().setOnEventReceived(this);
    AiRmote.getSocketClient().setOnSocketStateChanged(this);

    if (AiRmote.getSocketClient().isConnected()) {
      Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onEventReceived(Proto.Event event) {
  }

  Dialog mDialog;
  @Override
  public void onStateChanged(boolean connect, String message) {
    if (!connect) {
      mNsdHelper.discoverServices();
      if (mDialog == null) {
        final View contentView = getLayoutInflater().inflate(R.layout.connect_dialog, null);
        final ListView listView = ((ListView) contentView.findViewById(R.id.listView));
        listView.setAdapter(mNsdHelper.mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            System.out.println("MainActivity.onItemClick " + mNsdHelper.mAdapter.getItem(position).mHostName + " " + id);
          }
        });

        mDialog = new AlertDialog.Builder(this)
            .setTitle("Connect to inAiR")
            .setView(contentView)
            .setPositiveButton("Rescan", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                mNsdHelper.stopDiscovery();
                mNsdHelper.discoverServices();
              }
            })
            .create();

        mDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
          @Override
          public void onCancel(DialogInterface dialog) {
            mDialog = null;
          }
        });

        mDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(DialogInterface dialog) {
//            if (input.getText().length() <= 0) {
//              mDialog.show();
//            } else {
              mDialog = null;
//            }
          }
        });

        mDialog.show();
      }
    } else {
      mNsdHelper.stopDiscovery();
      Toast.makeText(this, "Connected " + message, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    mNsdHelper.registerService();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!AiRmote.getSocketClient().isConnected()) {
      AiRmote.getSocketClient().reconnectToLastHost();
    }
  }

  @Override
  protected void onPause() {
    mNsdHelper.stopDiscovery();
    AiRmote.getSocketClient().disconnect();
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    mNsdHelper.tearDown();
    super.onDestroy();
  }
}
