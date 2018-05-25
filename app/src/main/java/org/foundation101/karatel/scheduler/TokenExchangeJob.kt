package org.foundation101.karatel.scheduler

import android.util.Log
import com.evernote.android.job.Job
import org.foundation101.karatel.activity.MainActivity
import org.foundation101.karatel.activity.MainActivity.SignOutSender.buildResponseString
import org.foundation101.karatel.activity.MainActivity.SignOutSender.logoutPossible
import org.foundation101.karatel.activity.TipsActivity
import org.foundation101.karatel.manager.KaratelPreferences
import org.json.JSONObject

class TokenExchangeJob: Job() {
    companion object {
        const val TAG = "TokenExchangeJob"
    }

    override fun onRunJob(params: Params): Result {
        Log.d(TAG, "onRunJob ${this}")

        synchronized (KaratelPreferences.TAG) {
            //Part 1 - send login request with new push token
            val newGcmToken = KaratelPreferences.newPushToken()
            if (newGcmToken.isNotEmpty()) {
                val email = KaratelPreferences.lastLoginEmail()
                val passw = KaratelPreferences.password()
                if (passw.isEmpty()) { //the data is lost, no need to retry
                    KaratelPreferences.setPendingJob(null)
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

                        KaratelPreferences.setOldSessionToken(KaratelPreferences.sessionToken())
                        KaratelPreferences.setSessionToken(newSessionToken)

                        KaratelPreferences.setOldPushToken(KaratelPreferences.pushToken())
                        KaratelPreferences.setPushToken(newGcmToken)
                        KaratelPreferences.setNewPushToken(null)
                    } else {
                        return Job.Result.RESCHEDULE
                    }
                } catch (e: Exception) {
                    return Job.Result.RESCHEDULE
                }

            }

            //Part 2 - destroy the session with old push token
            val oldSessionToken = KaratelPreferences.oldSessionToken()
            val oldPushToken = KaratelPreferences.oldPushToken()

            //check conditions when we don't need signout
            if ((oldSessionToken.isEmpty()) ||
                (oldPushToken == KaratelPreferences.pushToken() && oldSessionToken == KaratelPreferences.sessionToken())) {
                //the data is lost, no need to retry
                KaratelPreferences.setPendingJob(null)
                return Job.Result.SUCCESS
            }

            return try {
                val json = MainActivity.SignOutSender.performSignOutRequest(oldSessionToken, oldPushToken)
                if (logoutPossible(buildResponseString(json))) {
                    KaratelPreferences.setOldSessionToken(null)
                    KaratelPreferences.setOldPushToken(null)
                    KaratelPreferences.setPendingJob(null)
                    Job.Result.SUCCESS
                } else Job.Result.RESCHEDULE
            } catch (e: Exception) {
                Job.Result.RESCHEDULE
            }
        }
    }
}