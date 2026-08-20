package com.safaul.encryptedphotolocker;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {

    private WebView webView;

    private ValueCallback<Uri[]> filePathCallback;

    private static final int FILE_CHOOSER_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        webView.setWebViewClient(new WebViewClient());

        webView.addJavascriptInterface(
        new AndroidBridge(),
        "Android"
);

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = callback;

                Intent intent =
                        new Intent(Intent.ACTION_OPEN_DOCUMENT);

                intent.addCategory(
                        Intent.CATEGORY_OPENABLE
                );

                intent.setType("*/*");

                startActivityForResult(
                        intent,
                        FILE_CHOOSER_REQUEST
                );

                return true;
            }
        });

        webView.loadUrl(
                "file:///android_asset/index.html"
        );

        setContentView(webView);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQUEST) {

            if (filePathCallback == null) {
                return;
            }

            Uri[] results = null;

            if (resultCode == RESULT_OK &&
                    data != null &&
                    data.getData() != null) {

                results =
                        new Uri[]{
                                data.getData()
                        };
            }

            filePathCallback.onReceiveValue(results);

            filePathCallback = null;
        }
    }

    // =================================================
    // SAVE FILE TO DOWNLOAD
    // =================================================
public void saveToDownloads(
        byte[] data,
        String fileName) {

    try {

        android.content.ContentValues values =
                new android.content.ContentValues();

        values.put(
                android.provider.MediaStore.Downloads.DISPLAY_NAME,
                fileName
        );

        values.put(
                android.provider.MediaStore.Downloads.MIME_TYPE,
                "application/octet-stream"
        );

        values.put(
                android.provider.MediaStore.Downloads.RELATIVE_PATH,
                android.os.Environment.DIRECTORY_DOWNLOADS
        );

        values.put(
                android.provider.MediaStore.Downloads.IS_PENDING,
                1
        );

        android.net.Uri uri =
                getContentResolver().insert(
                        android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        values
                );

        if (uri == null) {
            throw new Exception("Could not create file");
        }

        java.io.OutputStream output =
                getContentResolver().openOutputStream(uri);

        if (output == null) {
            throw new Exception("Could not open file");
        }

        output.write(data);
        output.flush();
        output.close();

        android.content.ContentValues complete =
                new android.content.ContentValues();

        complete.put(
                android.provider.MediaStore.Downloads.IS_PENDING,
                0
        );

        getContentResolver().update(
                uri,
                complete,
                null,
                null
        );

        Toast.makeText(
                this,
                "Saved in Downloads: " + fileName,
                Toast.LENGTH_LONG
        ).show();

    } catch (Exception e) {

        Toast.makeText(
                this,
                "Save failed: " + e.getMessage(),
                Toast.LENGTH_LONG
        ).show();
    }
}

    @Override
    public void onBackPressed() {

        if (webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }
public class AndroidBridge {

    @android.webkit.JavascriptInterface
    public void saveFile(String base64Data, String fileName) {

        try {

            byte[] data =
                    android.util.Base64.decode(
                            base64Data,
                            android.util.Base64.DEFAULT
                    );

            saveToDownloads(data, fileName);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Save failed: " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }
}
}
