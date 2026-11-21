package com.dticebu.spellingquiz;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {

    private static final String GOOGLE_TTS = "com.google.android.tts";

    private WebView webView;
    private TextToSpeech tts;
    private volatile boolean ttsReady = false;
    private boolean usingGoogle = false;

    // Voice chosen in the app's Voice settings (applied once the engine is ready)
    private volatile String accent = "US";
    private volatile String voiceName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#FFF8EC"));
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        getWindow().setNavigationBarColor(Color.parseColor("#FFF8EC"));

        // Prefer Google's speech engine (clearer than many phone makers' engines)
        boolean hasGoogle = false;
        try {
            getPackageManager().getPackageInfo(GOOGLE_TTS, 0);
            hasGoogle = true;
        } catch (Exception ignored) { }
        usingGoogle = hasGoogle;

        TextToSpeech.OnInitListener onInit = status -> {
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true;
                applyVoice();
            } else if (usingGoogle) {
                // Google engine failed: fall back to the phone's default engine
                usingGoogle = false;
                tts = new TextToSpeech(this, s2 -> {
                    if (s2 == TextToSpeech.SUCCESS) {
                        ttsReady = true;
                        applyVoice();
                    }
                });
            }
        };
        tts = hasGoogle ? new TextToSpeech(this, onInit, GOOGLE_TTS) : new TextToSpeech(this, onInit);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.parseColor("#FFF8EC"));
        webView.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidTTS");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private static boolean countryMatches(Locale l, String cc) {
        if (cc.equalsIgnoreCase(l.getCountry())) return true;
        try {
            String iso3 = l.getISO3Country();
            return (cc.equals("US") && iso3.equals("USA")) || (cc.equals("GB") && iso3.equals("GBR"))
                    || (cc.equals("AU") && iso3.equals("AUS")) || (cc.equals("IN") && iso3.equals("IND"));
        } catch (Exception e) {
            return false;
        }
    }

    /** Installed (offline) English voices for a country, best quality first. */
    private List<Voice> voicesFor(String cc) {
        List<Voice> out = new ArrayList<>();
        Set<Voice> all = null;
        try { all = tts.getVoices(); } catch (Exception ignored) { }
        if (all == null) return out;
        for (Voice v : all) {
            Locale l = v.getLocale();
            if (l == null || !"en".equals(l.getLanguage()) || !countryMatches(l, cc)) continue;
            if (v.isNetworkConnectionRequired()) continue;
            Set<String> f = v.getFeatures();
            if (f != null && f.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED)) continue;
            out.add(v);
        }
        out.sort((a, b) -> a.getQuality() != b.getQuality()
                ? b.getQuality() - a.getQuality()
                : a.getName().compareTo(b.getName()));
        return out;
    }

    private void applyVoice() {
        if (!ttsReady) return;
        try {
            Locale loc = new Locale("en", accent);
            int r = tts.setLanguage(loc);
            if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US);
            }
            List<Voice> vs = voicesFor(accent);
            Voice chosen = null;
            for (Voice v : vs) if (v.getName().equals(voiceName)) chosen = v;
            if (chosen == null && !vs.isEmpty()) chosen = vs.get(0);
            if (chosen != null) tts.setVoice(chosen);
            tts.setPitch(1.0f);
        } catch (Exception ignored) { }
    }

    /** Lets the quiz page read words aloud and choose a voice. */
    private class SpeechBridge {
        @JavascriptInterface
        public void speak(String text, float rate) {
            if (!ttsReady || text == null) return;
            tts.setSpeechRate(Math.max(0.3f, Math.min(rate, 1.5f)));
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word");
        }

        @JavascriptInterface
        public boolean ready() { return ttsReady; }

        /** JSON array of voice names for "US", "GB", "AU" or "IN". */
        @JavascriptInterface
        public String voices(String cc) {
            JSONArray arr = new JSONArray();
            if (!ttsReady || cc == null) return arr.toString();
            for (Voice v : voicesFor(cc)) arr.put(v.getName());
            return arr.toString();
        }

        @JavascriptInterface
        public void setVoice(String cc, String name) {
            accent = (cc == null || cc.isEmpty()) ? "US" : cc;
            voiceName = name == null ? "" : name;
            applyVoice();
        }

        @JavascriptInterface
        public String engine() {
            if (!ttsReady) return "";
            return usingGoogle ? "Google" : "Phone default";
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(() -> {
                try {
                    Intent i = new Intent("com.android.settings.TTS_SETTINGS");
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    try {
                        Intent i = new Intent(android.provider.Settings.ACTION_SETTINGS);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                    } catch (Exception ignored) { }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
