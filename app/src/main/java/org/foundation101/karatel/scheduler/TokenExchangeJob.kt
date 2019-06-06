package org.foundation101.karatel.scheduler

import android.util.Log
import com.evernote.android.job.Job
import org.foundation101.karatel.KaratelApplication
import org.foundation101.karatel.activity.MainActivity
import org.foundation101.karatel.activity.MainActivity.SignOutSender.buildResponseString
import org.foundation101.karatel.activity.MainActivity.SignOutSender.logoutPossible
import org.foundation101.karatel.activity.TipsActivity
import org.foundation101.karatel.manager.KaratelPreferences
import org.json.JSONObject
import javax.inject.Inject

class TokenExchangeJob: Job() {
    companion object {
        const val TAG = "TokenExchangeJob"
    }

    @Inject internal lateinit var preferences: KaratelPreferences

    override fun onRunJob(params: Params): Result {
        Log.d(TAG, "onRunJob ${this}")
        KaratelApplication.dagger().inject(this)

        synchronized (KaratelPreferences.TAG) {
            //Part 1 - send login request with new push token
            val newGcmToken = preferences.newPushToken()
            if (newGcmToken.isNotEmpty()) {
                val email = preferences.lastLoginEmail()
                val passw = preferences.password()
                if (passw.isEmpty()) { //the data is lost, no need to retry
                    preferences.setPendingJob(null)
                    return Job.Result.SUCCESS
                }
                val loggedWithFb = email.isEmpty()

                val loginSender = if (loggedWithFb) {
                    TipsActivity.LoginSender(null, passw, newGcmToken)
                } else {
                    TipsActivity.LoginSender(null, email, passw, newGcmToken)
                }
                try {
                    val loginWithNewToken_Result = TipsActivity.LoginSender.performLoginRequest(loginSender)

                    val json = JSONObject(loginWithNewToken_Result)
                    if (json.getString("status") == "success") {
                        val newSessionToken = json.getJSONObject("data").getString("token")

                        preferences.setOldSessionToken(preferences.sessionToken())
                        preferences.setSessionToken(newSessionToken)

                        preferences.setOldPushToken(preferences.pushToken())
                        preferences.setPushToken(newGcmToken)
                        preferences.setNewPushToken(null)
                    } else {
                        return Job.Result.RESCHEDULE
                    }
                } catch (e: Exception) {
                    return Job.Result.RESCHEDULE
                }

            }

            //Part 2 - destroy the session with old push token
            val oldSessionToken = preferences.oldSessionToken()
            val oldPushToken = preferences.oldPushToken()

            //check conditions when we don't need signout
            if ((oldSessionToken.isEmpty()) ||
                (oldPushToken == preferences.pushToken() && oldSessionToken == preferences.sessionToken())) {
                //the data is lost, no need to retry
                preferences.setPendingJob(null)
                return Job.Result.SUCCESS
            }

            return try {
                val json = MainActivity.SignOutSender.performSignOutRequest(oldSessionToken, oldPushToken)
                if (logoutPossible(buildResponseString(json))) {
                    preferences.setOldSessionToken(null)
                    preferences.setOldPushToken(null)
                    preferences.setPendingJob(null)
                    Job.Result.SUCCESS
                } else Job.Result.RESCHEDULE
            } catch (e: Exception) {
                Job.Result.RESCHEDULE
            }
        }
    }
}