package org.foundation101.karatel.asyncTasks;

import java.lang.ref.WeakReference;

public abstract class AsyncTaskAction<IN, OUT, T> {
    protected WeakReference<T> ref;
    protected ErrorHandler handler = new ErrorHandler();

    public AsyncTaskAction(T component) {
        ref = new WeakReference<>(component);
    }

    public AsyncTaskAction(T component, ErrorHandler handler) {
        ref = new WeakReference<>(component);
        this.handler = handler;
    }

    public abstract void pre (IN  arg);
    public abstract void post(OUT arg);

    public void onCancel() {}
}
