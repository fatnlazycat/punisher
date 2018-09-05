package org.foundation101.karatel.asyncTasks;

import java.lang.ref.WeakReference;

public abstract class AsyncTaskAction<IN, OUT, T> {
    protected WeakReference<T> ref;

    public AsyncTaskAction(T component) {
        ref = new WeakReference<>(component);
    }

    public abstract void pre (IN  arg);
    public abstract void post(OUT arg);
}
