package de.robv.android.xposed;

import java.io.File;
import java.util.Map;
import java.util.Set;

/** Compile-time stub of LSPosed XSharedPreferences (runtime provided by framework). */
public class XSharedPreferences implements android.content.SharedPreferences {
    public XSharedPreferences(File file) {}
    public XSharedPreferences(String packageName, String prefFile) {}
    public XSharedPreferences(String packageName, String prefFile, int mode) {}
    public void makeWorldReadable() {}
    public void reload() {}
    public void removeFile() {}

    @Override public Map<String, ?> getAll() { return null; }
    @Override public String getString(String k, String d) { return d; }
    @Override public Set<String> getStringSet(String k, Set<String> d) { return d; }
    @Override public int getInt(String k, int d) { return d; }
    @Override public long getLong(String k, long d) { return d; }
    @Override public float getFloat(String k, float d) { return d; }
    @Override public boolean getBoolean(String k, boolean d) { return d; }
    @Override public boolean contains(String k) { return false; }
    @Override public Editor edit() { return null; }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) {}
}