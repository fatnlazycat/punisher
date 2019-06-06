package org.foundation101.karatel.dagger;

import org.foundation101.karatel.activity.ChangeEmailActivity;
import org.foundation101.karatel.activity.ComplainActivity;
import org.foundation101.karatel.activity.ForgotPassword2Activity;
import org.foundation101.karatel.activity.MainActivity;
import org.foundation101.karatel.activity.SignUpActivity;
import org.foundation101.karatel.activity.TipsActivity;
import org.foundation101.karatel.activity.ViolationActivity;
import org.foundation101.karatel.adapter.DrawerAdapter;
import org.foundation101.karatel.asyncTasks.ProfileFetcher;
import org.foundation101.karatel.asyncTasks.ProfileSaver;
import org.foundation101.karatel.fragment.AboutFragment;
import org.foundation101.karatel.fragment.ChangeAvatarFragment;
import org.foundation101.karatel.fragment.ComplainDraftsFragment;
import org.foundation101.karatel.fragment.ProfileFragment;
import org.foundation101.karatel.manager.CameraManager;
import org.foundation101.karatel.manager.HttpHelper;
import org.foundation101.karatel.manager.KaratelLocationManager;
import org.foundation101.karatel.manager.PermissionManager;
import org.foundation101.karatel.scheduler.RegistrationRetryJob;
import org.foundation101.karatel.scheduler.TokenExchangeJob;
import org.foundation101.karatel.service.MyGcmListenerService;
import org.foundation101.karatel.service.RegistrationIntentService;
import org.foundation101.karatel.utils.MultipartUtility;

import dagger.Component;

/**
 * Created by Dima on 01.02.2018.
 */
@Component//(modules = PermissionManager.class)
public interface DaggerComponent {
    void inject(KaratelLocationManager target);
    void inject(CameraManager target);
    void inject(ChangeAvatarFragment target);

    void inject(ChangeEmailActivity target);
    void inject(ComplainActivity target);
    void inject(ForgotPassword2Activity target);
    void inject(MainActivity target);
    void inject(MainActivity.FacebookBinder target);
    void inject(MainActivity.SignOutSender target);
    void inject(SignUpActivity target);
    void inject(TipsActivity target);
    void inject(TipsActivity.AvatarGetter target);
    void inject(TipsActivity.LoginSender target);
    void inject(ViolationActivity target);

    void inject(ProfileFetcher target);
    void inject(ProfileSaver target);

    void inject(AboutFragment target);
    void inject(ComplainDraftsFragment target);
    void inject(ProfileFragment target);
    void inject(ProfileFragment.ProfileFetcherActions target);

    void inject(HttpHelper target);

    void inject(MyGcmListenerService target);
    void inject(RegistrationIntentService target);

    void inject(MultipartUtility target);

    void inject(RegistrationRetryJob target);
    void inject(TokenExchangeJob target);
}
