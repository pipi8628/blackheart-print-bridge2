package com.blackheart.utf8test;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.*;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

public class MainActivity extends Activity {
    private EditText printerIpInput, portInput;
    private TextView statusText, logText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("utf8_test_settings", MODE_PRIVATE);
        buildUi();
        loadSettings();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        root.setBackgroundColor(Color.rgb(17, 17, 17));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        root.addView(tv("🧪 GoDEX DX2 UTF-8 / 字型測試 APK", 26, Color.WHITE, true));
        root.addView(tv("這支只拿來測 EZPL 純文字中文，不會碰原本 POS/監聽/列印佇列。", 16, Color.rgb(255, 209, 102), false));

        statusText = tv("尚未測試", 20, Color.rgb(255, 209, 102), true);
        root.addView(statusText);

        root.addView(label("GoDEX IP"));
        printerIpInput = input("192.168.31.189");
        root.addView(printerIpInput);

        root.addView(label("Port"));
        portInput = input("9100");
        root.addView(portInput);

        root.addView(btn("1️⃣ 英文 AA TEST", Color.rgb(0, 140, 255), v -> sendTest("AA_EN_UTF8")));
        root.addView(btn("2️⃣ 中文 AA + UTF-8 + ^CI28", Color.rgb(6, 214, 160), v -> sendTest("AA_ZH_UTF8")));
        root.addView(btn("3️⃣ 中文 Az1 + UTF-8 + ^CI28", Color.rgb(255, 183, 3), v -> sendTest("AZ1_ZH_UTF8")));
        root.addView(btn("4️⃣ 中文 VF + UTF-8 + ^CI28", Color.rgb(255, 183, 3), v -> sendTest("VF_ZH_UTF8")));
        root.addView(btn("5️⃣ 中文 AA + Big5", Color.rgb(239, 71, 111), v -> sendTest("AA_ZH_BIG5")));
        root.addView(btn("6️⃣ 中文 AA + Big5 + ^CI28", Color.rgb(239, 71, 111), v -> sendTest("AA_ZH_BIG5_CI28")));

        root.addView(label("Log"));
        logText = tv("", 14, Color.WHITE, false);
        logText.setBackgroundColor(Color.rgb(43, 43, 43));
        logText.setPadding(16, 16, 16, 16);
        root.addView(logText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(scroll);
    }

    private TextView tv(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(0, 8, 0, 8);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }

    private TextView label(String text) {
        return tv(text, 18, Color.rgb(255, 209, 102), true);
    }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextColor(Color.BLACK);
        e.setTextSize(24);
        e.setBackgroundColor(Color.WHITE);
        e.setPadding(16, 12, 16, 12);
        return e;
    }

    private Button btn(String text, int bg, android.view.View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(19);
        b.setTextColor(Color.BLACK);
        b.setBackgroundColor(bg);
        b.setOnClickListener(l);
        return b;
    }

    private void loadSettings() {
        printerIpInput.setText(prefs.getString("printerIp", "192.168.31.189"));
        portInput.setText(prefs.getString("port", "9100"));
    }

    private void saveSettings() {
        prefs.edit()
                .putString("printerIp", printerIpInput.getText().toString().trim())
                .putString("port", portInput.getText().toString().trim())
                .apply();
    }

    private void sendTest(String mode) {
        saveSettings();
        new Thread(() -> {
            try {
                TestPayload p = buildPayload(mode);
                ui(() -> {
                    status("送出中：" + p.name);
                    log("模式：" + p.name + "\n編碼：" + p.charset + "\n指令：\n" + p.text);
                });
                sendSocket(p.text.getBytes(Charset.forName(p.charset)));
                ui(() -> status("已送出：" + p.name));
            } catch (Exception ex) {
                ui(() -> {
                    status("失敗：" + ex.getMessage());
                    log(ex.toString());
                });
            }
        }).start();
    }

    private TestPayload buildPayload(String mode) {
        String headerUtf8 = "^Q30,3\r\n^W40\r\n^H12\r\n^S2\r\n^CI28\r\n^L\r\n";
        String headerNoCi = "^Q30,3\r\n^W40\r\n^H12\r\n^S2\r\n^L\r\n";
        String end = "E\r\n";

        if ("AA_EN_UTF8".equals(mode)) {
            return new TestPayload("AA 英文 UTF-8", "UTF-8", headerNoCi +
                    "AA,20,40,2,2,0,0E,TEST\r\n" + end);
        }
        if ("AA_ZH_UTF8".equals(mode)) {
            return new TestPayload("AA 中文 UTF-8 + CI28", "UTF-8", headerUtf8 +
                    "AA,20,40,1,1,0,0E,黑心地瓜球\r\n" +
                    "AA,20,90,1,1,0,0E,無糖 少冰\r\n" +
                    "AA,20,140,1,1,0,0E,$80\r\n" + end);
        }
        if ("AZ1_ZH_UTF8".equals(mode)) {
            return new TestPayload("Az1 中文 UTF-8 + CI28", "UTF-8", headerUtf8 +
                    "AA,20,40,1,1,0,Az1,黑心地瓜球\r\n" +
                    "AA,20,90,1,1,0,Az1,無糖 少冰\r\n" +
                    "AA,20,140,1,1,0,Az1,$80\r\n" + end);
        }
        if ("VF_ZH_UTF8".equals(mode)) {
            return new TestPayload("VF 中文 UTF-8 + CI28", "UTF-8", headerUtf8 +
                    "AA,20,40,1,1,0,VF,黑心地瓜球\r\n" +
                    "AA,20,90,1,1,0,VF,無糖 少冰\r\n" +
                    "AA,20,140,1,1,0,VF,$80\r\n" + end);
        }
        if ("AA_ZH_BIG5".equals(mode)) {
            return new TestPayload("AA 中文 Big5", "Big5", headerNoCi +
                    "AA,20,40,1,1,0,0E,黑心地瓜球\r\n" +
                    "AA,20,90,1,1,0,0E,無糖 少冰\r\n" + end);
        }
        return new TestPayload("AA 中文 Big5 + CI28", "Big5", headerUtf8 +
                "AA,20,40,1,1,0,0E,黑心地瓜球\r\n" +
                "AA,20,90,1,1,0,0E,無糖 少冰\r\n" + end);
    }

    private void sendSocket(byte[] data) throws Exception {
        String ip = printerIpInput.getText().toString().trim();
        int port = Integer.parseInt(portInput.getText().toString().trim());
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(ip, port), 3000);
        socket.setSoTimeout(3000);
        OutputStream os = socket.getOutputStream();
        os.write(data);
        os.flush();
        Thread.sleep(200);
        os.close();
        socket.close();
    }

    private void ui(Runnable r) { handler.post(r); }
    private void status(String s) { statusText.setText(s); log(s); }
    private void log(String s) { logText.setText(s + "\n\n" + logText.getText().toString()); }

    private static class TestPayload {
        final String name;
        final String charset;
        final String text;
        TestPayload(String name, String charset, String text) {
            this.name = name;
            this.charset = charset;
            this.text = text;
        }
    }
}
