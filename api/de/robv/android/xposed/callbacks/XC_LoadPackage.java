package de.robv.android.xposed.callbacks;

import de.robv.android.xposed.XC_MethodHook;

/** Compile-time stub. Real implementation provided by LSPosed/XposedBridge at runtime. */
public abstract class XC_LoadPackage extends XC_MethodHook {
    public static class LoadPackageParam {
        public String packageName;
        public String processName;
        public ClassLoader classLoader;
        public XC_LoadPackage lpparam;
        public LoadPackageParam() {}
    }
    public XC_LoadPackage() { super(); }
    public XC_LoadPackage(int priority) { super(priority); }
}