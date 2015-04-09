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

import inair.eventcenter.proto.Helper;
import inair.eventcenter.proto.Proto;
import tv.inair.airmote.connection.OnEventReceived;
import tv.inair.airmote.connection.OnSocketStateChanged;
import tv.inair.airmote.connection.SocketClient;
import tv.inair.airmote.connection.WifiAdapter;
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

  public static final int REQUEST_WIFI_SETUP = 1;
  public static final int REQUEST_SCAN_INAIR = 2;
  public static final int REQUEST_TEXT_INPUT = 3;

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

    mClient.addEventReceivedListener(this);
    mClient.addSocketStateChangedListener(this);

    if (!mClient.isConnected()) {
      mClient.quickConnect();
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

    view.findViewById(R.id.helpBtn).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        showHelp();
      }
    });

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

        if (!Application.getSettingsPreferences().contains(Application.FIRST_TIME_KEY)) {
          Application.getSettingsPreferences().edit().putBoolean(Application.FIRST_TIME_KEY, false).apply();
          settingDevice();
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
    mClient.ensureNotConnectToWifiDirect();
    hideControlView();
  }

  @Override
  public void onDestroyView() {
    mGestureControl.setListener(null);
    super.onDestroyView();
  }

  @Override
  public void onDetach() {
    super.onDetach();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_WIFI_SETUP:
        if (resultCode == Activity.RESULT_OK) {
          String ssid = data.getStringExtra(WifiListActivity.EXTRA_SSID);
          String bssid = data.getStringExtra(WifiListActivity.EXTRA_BSSID);
          String capabilities = data.getStringExtra(WifiListActivity.EXTRA_CAPABILITIES);
          String password = data.getStringExtra(WifiListActivity.EXTRA_PASSWORD);
          WifiAdapter.connectWifiTo(ssid, bssid, capabilities, password);
        }
        break;

      case REQUEST_SCAN_INAIR:
        if (resultCode == Activity.RESULT_OK) {

        }
        break;
    }
  }

  public boolean onBackPressed() {
    if (mGuideImage.getVisibility() == View.VISIBLE) {
      mGuideImage.setVisibility(View.GONE);
      mClient.changeToSettingMode(false);
      return true;
    }
    return false;
  }
  //endregion

  //region Buttons
  private void showHelp() {
    Intent i = new Intent(getActivity(), WebViewActivity.class);
    i.putExtra(WebViewActivity.EXTRA_URL, getResources().getString(R.string.help_url));
    startActivity(i);
  }

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
    Intent i = new Intent(getActivity(), DeviceListActivity.class);
    startActivityForResult(i, REQUEST_SCAN_INAIR);
  }

  private void switchDisplayMode() {
    mClient.sendEvent(Helper.newFunctionEvent(Proto.FunctionEvent.F4));
  }

  private void settingDevice() {
    mGuideImage.setVisibility(View.VISIBLE);
    mClient.changeToSettingMode(true);
  }
  //endregion

  //region Implement
  private void processTextInput(Proto.Event event) {
    Proto.TextInputRequestEvent textInputEvent = event.getExtension(Proto.TextInputRequestEvent.event);
    assert textInputEvent != null;
    Intent i = new Intent(getActivity(), TextInputActivity.class);
    i.putExtra(TextInputActivity.EXTRA_REPLY_TO, event.replyTo);
    i.putExtra(TextInputActivity.EXTRA_MAX_LENGTH, textInputEvent.maxLength);
    startActivityForResult(i, REQUEST_TEXT_INPUT);
  }

  private void processWebView(Proto.Event event, boolean isWebView) {
    Intent i = new Intent(getActivity(), WebViewActivity.class);
    i.putExtra(WebViewActivity.EXTRA_REPLY_TO, event.replyTo);

    if (isWebView) {
      Proto.WebViewRequestEvent webViewEvent = event.getExtension(Proto.WebViewRequestEvent.event);
      assert webViewEvent != null;
      i.putExtra(WebViewActivity.EXTRA_URL, webViewEvent.url);
    } else {
      Proto.OAuthRequestEvent oAuthEvent = event.getExtension(Proto.OAuthRequestEvent.event);
      assert oAuthEvent != null;
      i.putExtra(WebViewActivity.EXTRA_URL, oAuthEvent.authUrl);
    }

    startActivity(i);
  }

  @Override
  public void onEventReceived(Proto.Event event) {
    if (!isVisible()) {
      return;
    }
    if (event != null && event.type != null) {
      switch (event.type) {
        case Proto.Event.WEBVIEW_REQUEST:
          processWebView(event, true);
          break;

        case Proto.Event.OAUTH_REQUEST:
          processWebView(event, false);
          break;

        case Proto.Event.TEXT_INPUT_REQUEST:
          processTextInput(event);
          break;
      }
    }
  }

  @Override
  public void onStateChanged(boolean connect, String message) {
    if (!isVisible()) {
      return;
    }
    if (connect) {
      mGuideImage.setVisibility(View.GONE);
      if (mClient.isInSettingMode()) {
        Intent i = new Intent(getActivity(), WifiListActivity.class);
        startActivityForResult(i, REQUEST_WIFI_SETUP);
      }
    }
  }

  @Override
  public void onEvent() {
    hideControlView();
  }
  //endregion
}
