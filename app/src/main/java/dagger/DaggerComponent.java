package dagger;

import android.app.Activity;

import org.foundation101.karatel.manager.PermissionManager;

/**
 * Created by Dima on 01.02.2018.
 */
@Component(modules = PermissionManager.class)
public interface DaggerComponent {
    void inject(Object target);
}
