package org.foundation101.karatel.dagger;

import org.foundation101.karatel.fragment.ChangeAvatarFragment;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.KaratelLocationManager;
import org.foundation101.karatel.manager.PermissionManager;

import dagger.Component;

/**
 * Created by Dima on 01.02.2018.
 */
@Component(modules = PermissionManager.class)
public interface DaggerComponent {
    void inject(KaratelLocationManager target);
    void inject(CameraManager target);
    void inject(ChangeAvatarFragment target);
}
