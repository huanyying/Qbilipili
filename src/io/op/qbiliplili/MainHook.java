package io.op.qbiliplili;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * QbiliPlili - redirect QQ's bilibili mini-program cards to PiliPlus.
 *  - Detects bilibili mini-program (miniAppId=1109937557) via KEY_APPINFO.launchParam.
 *  - Extracts bvid from launchParam.entryPath.
 *  - Opens bilibili://video/{bvid} in PiliPlus, blocks original mini-program start.
 *  - Suppresses QQ's "leaving QQ" confirm dialog (QQCustomDialog) at show().
 */
public class MainHook implements IXposedHookLoadPackage {

    static final String QQ = "com.tencent.mobileqq";
    // write log to QQ's own data dir (QQ process can write; root can read)
    static final String LOGDIR = "/data/user/0/com.tencent.mobileqq/files/qbiliplili";
    static final String LOGFILE = LOGDIR + "/log.txt";
    // PiliPlus registers bilibili://video (video deep link) authority
    static final String PLILI_PREFIX = "bilibili://video/";
    // QQ bilibili mini-program appId
    static final String BILI_MINI_APPID = "1109937557";

    static final Pattern BVID = Pattern.compile("(?i)(?:bvid=)(BV[0-9A-Za-z]{10})");
    static DialogInterface.OnClickListener sPositiveBtn = null;
    static boolean sRedirecting = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!QQ.equals(lpparam.packageName)) return;
        log("=== module loaded, cl=" + lpparam.classLoader);

        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam p) {
                try {
                    Intent intent = null;
                    if (p.args != null && p.args.length > 0 && p.args[0] instanceof Intent) {
                        intent = (Intent) p.args[0];
                    } else if (p.args != null && p.args.length > 1 && p.args[1] instanceof Intent) {
                        intent = (Intent) p.args[1];
                    }
                    if (intent == null) return;

                    // quick reject: only care about bilibili mini-program cards
                    if (!isBiliMini(intent)) return;

                    // target hit -> now log (small) and extract bvid
                    log("MINI: " + dump(intent));

                    String bvid = extractBvid(reflectLaunchParam(intent));
                    if (bvid != null) {
                        if (isEnabled()) {
                            redirect(p, bvid);
                        } else {
                            log(">>> DISABLED, bvid=" + bvid);
                        }
                    } else {
                        log(">>> no bvid in launchParam");
                    }
                } catch (Throwable t) {
                    log("hook err: " + t);
                }
            }
        };

        ClassLoader cl = lpparam.classLoader;
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.content.ContextWrapper", cl), "startActivity", hook);
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.content.ContextWrapper", cl), "startActivityForResult", hook);
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Activity", cl), "startActivity", hook);
        XposedBridge.hookAllMethods(XposedHelpers.findClass("android.app.Activity", cl), "startActivityForResult", hook);

        // auto-confirm QQ's "leaving QQ" dialog (QQCustomDialog) to skip the popup
        try {
            Class<?> qqd = XposedHelpers.findClass("com.tencent.mobileqq.utils.QQCustomDialog", cl);
            // capture the confirm (positive) button click listener registered by QQ
            XposedBridge.hookAllMethods(qqd, "setPositiveButton", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    try {
                        if (p.args != null && p.args.length > 0 && p.args[p.args.length - 1] instanceof DialogInterface.OnClickListener) {
                            sPositiveBtn = (DialogInterface.OnClickListener) p.args[p.args.length - 1];
                        }
                    } catch (Throwable ignore) {}
                }
            });
            XposedBridge.hookAllMethods(qqd, "show", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    try {
                        if (sRedirecting && sPositiveBtn != null) {
                            log("QQD pre-confirm");
                            android.app.Dialog d = (android.app.Dialog) p.thisObject;
                            sPositiveBtn.onClick(d, DialogInterface.BUTTON_POSITIVE);
                            p.setResult(null); // block the popup from showing
                            sRedirecting = false;
                        }
                    } catch (Throwable ignore) {}
                }
            });
        } catch (Throwable t) { log("QQD no class: " + t); }
        log("=== hooks installed");
    }

    /** enabled via toggle file in QQ's own data dir (read by QQ itself, no cross-app issue), default OFF. */
    boolean isEnabled() {
        try {
            File f = new File("/data/user/0/com.tencent.mobileqq/files/qbiliplili/enabled");
            if (!f.exists()) { log("cfg: no file -> disabled"); return false; }
            java.util.Scanner s = new java.util.Scanner(f).useDelimiter("\\A");
            String c = s.hasNext() ? s.next().trim() : "";
            log("cfg: enabled=" + c);
            return "1".equals(c);
        } catch (Throwable t) {
            log("cfg: EXC " + t);
            return false;
        }
    }

    /** true if this is the bilibili QQ mini-program card launch. */
    boolean isBiliMini(Intent i) {
        try {
            if (i.getExtras() == null) return false;
            Object appInfo = i.getExtras().get("KEY_APPINFO");
            if (appInfo == null) return false;
            String launch = reflectLaunchParam(i);
            return launch != null && launch.contains("miniAppId='" + BILI_MINI_APPID + "'");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Reflect MiniAppInfo.launchParam.entryPath string, or null. */
    String reflectLaunchParam(Intent i) {
        try {
            if (i.getExtras() == null) return null;
            Object appInfo = i.getExtras().get("KEY_APPINFO");
            if (appInfo == null) return null;
            for (java.lang.reflect.Field f : appInfo.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object v = f.get(appInfo);
                    if (v != null && f.getName().equalsIgnoreCase("launchParam")) {
                        // return whole LaunchParam toString: holds miniAppId AND entryPath(bvid)
                        return String.valueOf(v);
                    }
                } catch (Throwable ignore) {}
            }
        } catch (Throwable t) {}
        return null;
    }

    String extractBvid(String s) {
        if (s == null) return null;
        Matcher m = BVID.matcher(s);
        return m.find() ? m.group(1) : null;
    }
    void redirect(XC_MethodHook.MethodHookParam p, String bvid) {
        try {
            Context ctx = (Context) p.thisObject;
            sRedirecting = true;
            Intent plili = new Intent(Intent.ACTION_VIEW, Uri.parse(PLILI_PREFIX + bvid));
            plili.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(plili);
            p.setResult(null); // block original mini-program
            log(">>> REDIRECTED to PiliPlus bvid=" + bvid);
        } catch (Throwable t) {
            log(">>> redirect failed: " + t);
        }
    }

    String safeStrExtra(Intent i, String key) {
        try { return i.getStringExtra(key); } catch (Throwable t) { return null; }
    }

    String dump(Intent i) {
        StringBuilder sb = new StringBuilder();
        sb.append("action=").append(i.getAction());
        sb.append(" data=").append(i.getData());
        sb.append(" cmp=").append(i.getComponent());
        sb.append(" flags=0x").append(Integer.toHexString(i.getFlags()));
        try {
            if (i.getExtras() != null) {
                String cmp = String.valueOf(i.getComponent());
                if (cmp.toLowerCase().contains("mini") || cmp.toLowerCase().contains("appbrand")) {
                    sb.append(" EXTRAS{");
                    for (String k : i.getExtras().keySet()) {
                        sb.append(k).append("=").append(i.getExtras().get(k)).append("; ");
                    }
                    sb.append("}");
                    try {
                        Object appInfo = i.getExtras().get("KEY_APPINFO");
                        if (appInfo != null) {
                            sb.append(" APPINFO_FIELDS{");
                            java.lang.reflect.Field[] fs = appInfo.getClass().getDeclaredFields();
                            for (java.lang.reflect.Field f : fs) {
                                try {
                                    f.setAccessible(true);
                                    Object v = f.get(appInfo);
                                    if (v != null && !(v instanceof java.util.Collection) && !(v instanceof java.util.Map))
                                        sb.append(f.getName()).append("=").append(v).append("; ");
                                } catch (Throwable ignore) {}
                            }
                            sb.append("}");
                        }
                    } catch (Throwable ignore) {}
                } else {
                    sb.append(" url=").append(safeStrExtra(i, "url"));
                    sb.append(" appid=").append(safeStrExtra(i, "appid"));
                    sb.append(" path=").append(safeStrExtra(i, "path"));
                }
            }
        } catch (Throwable ignore) {}
        return sb.toString();
    }

    static void log(String s) {
        try {
            XposedBridge.log("[QbiliPlili] " + s);
        } catch (Throwable ignore) {}
        try {
            File d = new File(LOGDIR);
            if (!d.exists()) d.mkdirs();
            FileWriter f = new FileWriter(LOGFILE, true);
            f.write(new Date().toString() + " | " + s + "\n");
            f.close();
        } catch (Throwable t) {
            try { XposedBridge.log("[QbiliPlili] filelog fail: " + t); } catch (Throwable ignore) {}
        }
    }
}
