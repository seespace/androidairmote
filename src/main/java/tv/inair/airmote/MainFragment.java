package tv.inair.airmote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.OnSocketStateChanged;
import tv.inair.airmote.connection.SocketClient;
import tv.inair.airmote.remote.GestureControl;
import tv.inair.airmote.utils.BitmapHelper;

/**
 * <p>
 * Note this class is currently under early design and development.
 * The API will likely change in later updates of the compatibility library,
 * requiring changes to the source code of apps when they are compiled against the newer version.
 * </p>
 * <p/>
 * <p>Copyright (c) 2015 SeeSpace.co. All rights reserved.</p>
 */
public class MainFragment extends Fragment implements OnEventReceived, OnSocketStateChanged, GestureControl.Listener {
  private GestureControl mGestureControl;
  private View mRootView;
  private View mControlView;
  private View mControlContainer;
  private ImageView mGuideImage;

  private SocketClient mClient;

  //region Override parent
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mClient = Application.getSocketClient();

    mClient.register(this);
    mClient.addEventReceivedListener(this);
    mClient.addSocketStateChangedListener(this);

    if (mClient.isConnected()) {
      Toast.makeText(getActivity(), "Connected " + mClient.getDisplayName(), Toast.LENGTH_SHORT).show();
    } else {
      mClient.reconnectToLastDevice();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main, container, false);

    mRootView = view.findViewById(R.id.rootView);
    mControlView = view.findViewById(R.id.controlView);
    mControlContainer = view.findViewById(R.id.controlContainer);
    mGuideImage = ((ImageView) view.findViewById(R.id.imageView));

    final ImageView moreBtn = ((ImageView) view.findViewById(R.id.moreBtn));
    final ImageView scan = ((ImageView) view.findViewById(R.id.scan));
    final ImageView mode2d3d = ((ImageView) view.findViewById(R.id.mode2d3d));
    final ImageView settings = ((ImageView) view.findViewById(R.id.settings));

    view.findViewById(R.id.moreBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleControlView();
      }
    });

    view.findViewById(R.id.scanBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        handleScanDevices();
        hideControlView();
      }
    });

    view.findViewById(R.id.threeDBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        switchDisplayMode();
        hideControlView();
      }
    });

    view.findViewById(R.id.settingBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        settingDevice();
        hideControlView();
      }
    });

    ViewTreeObserver vto = mRootView.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        ViewTreeObserver obs = mRootView.getViewTreeObserver();
        obs.removeOnGlobalLayoutListener(this);

        BitmapHelper.loadImageIntoView(getResources(), R.drawable.remote_guide, mGuideImage);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.more, moreBtn);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.mode2d3d, mode2d3d);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.scan, scan);
        BitmapHelper.loadImageIntoView(getResources(), R.drawable.settings, settings);

        System.out.println("MainFragment " + MainFragment.this);

        if (!Application.getSettingsPreferences().contains(Application.FIRST_TIME_KEY)) {
          mGuideImage.setVisibility(View.VISIBLE);
          Application.getSettingsPreferences().edit().putBoolean(Application.FIRST_TIME_KEY, false).apply();
        } else {
          mGuideImage.setVisibility(View.GONE);
        }

        hideControlView();
      }
    });

    mGestureControl = new GestureControl(getActivity(), mRootView);
    mGestureControl.setListener(this);

    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    hideControlView();
  }

  @Override
  public void onDestroyView() {
    mGestureControl.setListener(null);
    super.onDestroyView();
  }

  @Override
  public void onDetach() {
    mClient.unregister();
    super.onDetach();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
  }

  public boolean onBackPressed() {
    if (mGuideImage.getVisibility() == View.VISIBLE) {
      mGuideImage.setVisibility(View.GONE);
      return true;
    }
    return false;
  }
  //endregion

  //region Buttons
  private boolean isShow = true;

  private void toggleControlView() {
    if (isShow) {
      hideControlView();
    } else {
      showControlView();
    }
  }

  private void showControlView() {
    isShow = true;
    mControlView.animate().translationY(0).setDuration(250);
  }

  private void hideControlView() {
    isShow = false;
    mControlView.animate().translationY(mControlContainer.getHeight()).setDuration(250);
  }

  private void handleScanDevices() {
  }

  private void switchDisplayMode() {
    Toast.makeText(getActivity(), "Not implemented yet", Toast.LENGTH_SHORT).show();
  }

  private void settingDevice() {
    mClient.disconnect();
    mGuideImage.setVisibility(View.VISIBLE);
    mClient.changeToSettingMode(true);
  }
  //endregion

  //region Implement
  private void processOAuth(Proto.Event event) {
    Proto.OAuthRequestEvent oAuthEvent = event.getExtension(Proto.OAuthRequestEvent.event);
    assert oAuthEvent != null;
    Intent i = new Intent(getActivity(), WebviewActivity.class);
    i.putExtra(WebviewActivity.EXTRA_URL, oAuthEvent.authUrl);
    i.putExtra(WebviewActivity.EXTRA_REPLY_TO, event.replyTo);
    startActivity(i);
  }

  private void processTextInput(Proto.Event event) {
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (event != null && event.type != null) {
      switch (event.type) {
        case Proto.Event.OAUTH_REQUEST:
          processOAuth(event);
          break;
        case Proto.Event.TEXT_INPUT_REQUEST:
          processTextInput(event);
          break;

        default:
          System.out.println(event.type);
      }
    }
  }

  @Override
  public void onStateChanged(boolean connect, String message) {
    System.out.println("MainFragment.onStateChanged " + connect + " " + message);
    if (connect) {
      if (getActivity() != null) {
        Toast.makeText(getActivity(), "Connected " + message, Toast.LENGTH_SHORT).show();
        mGuideImage.setVisibility(View.GONE);
//        if (mSetting) {
          Intent i = new Intent(getActivity(), WifiListActivity.class);
          startActivity(i);
        //        }
      }
    }
  }

  @Override
  public void onEvent() {
    hideControlView();
  }
  //endregion
}
