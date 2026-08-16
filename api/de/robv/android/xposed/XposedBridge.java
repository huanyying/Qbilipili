package de.robv.android.xposed;

import java.util.Set;

/** Compile-time stub matching the real LSPosed API signatures. */
public final class XposedBridge {
    public static void log(String text) {}
    public static void log(Throwable t) {}
    public static Set hookAllMethods(Class<?> clazz, String methodName, XC_MethodHook callback) { return null; }
    public static Set hookAllConstructors(Class<?> clazz, XC_MethodHook callback) { return null; }
}