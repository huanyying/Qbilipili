package io.op.qbiliplili;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.io.File;

/** Settings entry — Material / MIUI style, theme: system / light / dark. Default OFF. */
public class ConfigActivity extends Activity {
    static final String QQ_DIR = "/data/user/0/com.tencent.mobileqq/files/qbiliplili";
    static final String KEY_THEME = "theme_mode"; // system | light | dark

    SharedPreferences prefs;
    boolean dark;

    // palette
    int bg, card, textMain, textSub, divider, accent;

    @Override
    protected void onCreate(Bundle b) {
        prefs = getSharedPreferences("config", Context.MODE_PRIVATE);
        dark = isDark();
        if (dark) setTheme(android.R.style.Theme_Material_NoActionBar);
        else setTheme(android.R.style.Theme_Material_Light_NoActionBar);
        super.onCreate(b);

        if (dark) { bg = 0xFF121212; card = 0xFF1E1E1E; textMain = 0xFFE3E3E3; textSub = 0xFF9AA0A6; divider = 0xFF2A2A2A; accent = 0xFF82AAFF; }
        else      { bg = 0xFFF4F5F7; card = 0xFFFFFFFF; textMain = 0xFF1A1D24; textSub = 0xFF8A919F; divider = 0xFFEDEFF2; accent = 0xFF3B82F6; }

        // light/dark status bar
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().setStatusBarColor(bg);
        if (!dark && android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        // ---- AppBar ----
        FrameLayout appBar = new FrameLayout(this);
        appBar.setBackgroundColor(card);
        appBar.setElevation(dp(2));
        FrameLayout.LayoutParams lpBar = new FrameLayout.LayoutParams(-1, dp(56));
        root.addView(appBar, lpBar);

        TextView barTitle = new TextView(this);
        barTitle.setText("QbiliPlili");
        barTitle.setTextSize(18);
        barTitle.setTextColor(textMain);
        barTitle.setTypeface(Typeface.DEFAULT_BOLD);
        barTitle.setGravity(Gravity.CENTER);
        appBar.addView(barTitle, new FrameLayout.LayoutParams(-1, -1));

        // ---- content ----
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(24));
        sv.addView(content);
        root.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1f));

        // ---- settings card ----
        LinearLayout setCard = card();
        content.addView(setCard);

        // 启用跳转 row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));
        setCard.addView(row);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tTitle = new TextView(this);
        tTitle.setText("启用跳转");
        tTitle.setTextSize(16);
        tTitle.setTextColor(textMain);
        texts.addView(tTitle);

        TextView tSub = new TextView(this);
        tSub.setText("点开 QQ 哔站卡片时改用 PiliPlus 打开视频");
        tSub.setTextSize(13);
        tSub.setTextColor(textSub);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(-1, -2);
        lpSub.topMargin = dp(4);
        texts.addView(tSub, lpSub);

        final Switch sw = new Switch(this);
        sw.setChecked(isEnabled());
        row.addView(sw);

        final TextView status = new TextView(this);
        status.setTextSize(12);
        status.setTextColor(accent);
        status.setPadding(dp(16), 0, dp(16), dp(12));
        setCard.addView(status);

        // initial root status
        if (detectRoot()) status.setText(isEnabled() ? "\u5df2\u542f\u7528" : "\u5df2\u5173\u95ed");
        else status.setText("\u26a0 root \u672a\u6388\u6743\uff0c\u8bf7\u5728 KernelSU \u6388\u6743\u540e\u4f7f\u7528\u5f00\u5173");

        sw.setOnCheckedChangeListener((button, checked) -> {
            boolean ok = writeToggle(checked);
            if (!ok) status.setText("\u26a0 root \u6388\u6743\u672a\u751f\u6548\uff0c\u5207\u6362\u5931\u8d25");
            else status.setText(checked ? "\u5df2\u542f\u7528" : "\u5df2\u5173\u95ed");
        });

        setCard.addView(divider());

        // 界面风格 row (clickable)
        LinearLayout themeRow = new LinearLayout(this);
        themeRow.setOrientation(LinearLayout.HORIZONTAL);
        themeRow.setGravity(Gravity.CENTER_VERTICAL);
        themeRow.setPadding(dp(16), dp(16), dp(16), dp(16));
        themeRow.setOnClickListener(v -> pickTheme());
        setCard.addView(themeRow);

        TextView thTitle = new TextView(this);
        thTitle.setText("界面风格");
        thTitle.setTextSize(16);
        thTitle.setTextColor(textMain);
        themeRow.addView(thTitle, new LinearLayout.LayoutParams(0, -2, 1f));

        final TextView thValue = new TextView(this);
        thValue.setText(themeName());
        thValue.setTextSize(14);
        thValue.setTextColor(textSub);
        themeRow.addView(thValue);

        // ---- about card ----
        LinearLayout about = card();
        LinearLayout.LayoutParams lpAbout = new LinearLayout.LayoutParams(-1, -2);
        lpAbout.topMargin = dp(12);
        content.addView(about, lpAbout);

        about.addView(aboutRow("版本", "1.0"));
        about.addView(divider());
        about.addView(aboutRow("模块作用域", "com.tencent.mobileqq"));
        about.addView(divider());
        about.addView(aboutRow("生效方式", "立即生效，无需重启 QQ"));

        TextView foot = new TextView(this);
        foot.setText("功能默认关闭，开启后在 QQ 内点开哔站卡片测试。");
        foot.setTextSize(12);
        foot.setTextColor(textSub);
        foot.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpFoot = new LinearLayout.LayoutParams(-1, -2);
        lpFoot.topMargin = dp(14);
        content.addView(foot, lpFoot);

        setContentView(root);
    }

    boolean isDark() {
        String m = prefs.getString(KEY_THEME, "system");
        if ("dark".equals(m)) return true;
        if ("light".equals(m)) return false;
        int nm = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return nm == Configuration.UI_MODE_NIGHT_YES;
    }

    String themeName() {
        String m = prefs.getString(KEY_THEME, "system");
        if ("dark".equals(m)) return "深色";
        if ("light".equals(m)) return "浅色";
        return "跟随系统";
    }

    void pickTheme() {
        final String[] names = {"跟随系统", "浅色", "深色"};
        final String[] vals = {"system", "light", "dark"};
        int cur = 0;
        String m = prefs.getString(KEY_THEME, "system");
        if ("light".equals(m)) cur = 1; else if ("dark".equals(m)) cur = 2;
        new AlertDialog.Builder(this)
            .setTitle("界面风格")
            .setSingleChoiceItems(names, cur, (d, w) -> {
                prefs.edit().putString(KEY_THEME, vals[w]).apply();
                recreate();
            })
            .setNegativeButton("取消", null)
            .show();
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable g = new GradientDrawable();
        g.setColor(card);
        g.setCornerRadius(dp(16));
        c.setBackground(g);
        c.setElevation(dp(1));
        return c;
    }

    View divider() {
        View v = new View(this);
        v.setBackgroundColor(divider);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
        lp.leftMargin = dp(16); lp.rightMargin = dp(16);
        v.setLayoutParams(lp);
        return v;
    }

    LinearLayout aboutRow(String k, String v) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView tk = new TextView(this);
        tk.setText(k); tk.setTextSize(14); tk.setTextColor(textMain);
        r.addView(tk, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView tv = new TextView(this);
        tv.setText(v); tv.setTextSize(13); tv.setTextColor(textSub);
        r.addView(tv);
        return r;
    }

    boolean isEnabled() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "cat " + QQ_DIR + "/enabled 2>/dev/null"});
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.destroy();
            return "1".equals(line == null ? "" : line.trim());
        } catch (Throwable t) { return false; }
    }

    boolean detectRoot() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String line = r.readLine();
            p.destroy();
            return line != null && line.contains("uid=0");
        } catch (Throwable t) { return false; }
    }

    boolean writeToggle(boolean on) {
        String v = on ? "1" : "0";
        try {
            Process p = Runtime.getRuntime().exec(new String[]{
                "su", "-c",
                "mkdir -p " + QQ_DIR + " && echo -n " + v + " > " + QQ_DIR + "/enabled && chmod 666 " + QQ_DIR + "/enabled && cat " + QQ_DIR + "/enabled"
            });
            java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(p.getInputStream()));
            String out = r.readLine();
            int exit = p.waitFor();
            return exit == 0 && v.equals(out == null ? "" : out.trim());
        } catch (Throwable t) { return false; }
    }

    int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}