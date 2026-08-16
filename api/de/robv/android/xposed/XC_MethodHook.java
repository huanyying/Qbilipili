package de.robv.android.xposed;

/** Compile-time stub. */
public class XC_MethodHook {
    public XC_MethodHook() {}
    public XC_MethodHook(int priority) {}

    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {}
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {}

    public static class MethodHookParam {
        public Object thisObject;
        public Object[] args;
        public Object result;
        public Throwable throwable;
        public boolean returnEarly = false;

        public Object getResult() { return result; }
        public void setResult(Object result) { this.result = result; this.throwable = null; this.returnEarly = true; }
        public void setThrowable(Throwable throwable) { this.throwable = throwable; this.returnEarly = true; }
        public Throwable getThrowable() { return throwable; }
        public Object getResultOrThrowable() throws Throwable {
            if (throwable != null) throw throwable;
            return result;
        }
    }

    public static class Unhook {
        public void unhook() {}
    }

    public abstract static class Unhooker {
        public abstract void unhook();
    }
}