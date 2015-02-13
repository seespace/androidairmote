package tv.inair.airmote;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.Set;

import tv.inair.airmote.remote.Helper;

public class WebviewActivity extends Activity {

  public static final String EXTRA_URL = "extra_url";
  public static final String EXTRA_REPLY_TO = "extra_reply_to";

  private static final String OAUTH_SERVER = "oauth.inair.tv";

  private WebView webView;

  private String url;
  private String replyTo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_webview);
    Intent i = getIntent();
    url = i.getStringExtra(EXTRA_URL);
    replyTo = i.getStringExtra(EXTRA_REPLY_TO);

    webView = ((WebView) findViewById(R.id.webView));
    webView.setWebViewClient(new Client());
    webView.getSettings().setJavaScriptEnabled(true);

    webView.loadUrl(url);
  }

  private class Client extends WebViewClient {
    @Override
    public void onPageFinished(WebView view, String url) {
      System.out.println(url);
      Uri uri = Uri.parse(url);
      if (OAUTH_SERVER.equalsIgnoreCase(uri.getHost())) {
        String code = null;
        Set<String> queryParamNames = uri.getQueryParameterNames();
        if (queryParamNames.contains("code")) {
          code = uri.getQueryParameter("code");
        } else if (queryParamNames.contains("oauth_verifier")) {
          code = uri.getQueryParameter("oauth_verifier");
        }
        if (code != null) {
          System.out.println("Client.onPageFinished " + code);
          Application.getSocketClient().sendEvent(Helper.newOAuthResponseEvent(code, replyTo));
        }
        WebviewActivity.this.finish();
      }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
      System.out.println("Client.onReceivedError " + description + " " + failingUrl);
      WebviewActivity.this.finish();
    }
  }
}
