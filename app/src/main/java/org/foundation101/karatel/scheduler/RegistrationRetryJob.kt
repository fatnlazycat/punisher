package org.foundation101.karatel.scheduler

import android.util.Log
import com.evernote.android.job.Job
import com.splunk.mint.Mint
import org.foundation101.karatel.KaratelApplication
import org.foundation101.karatel.manager.KaratelPreferences
import org.foundation101.karatel.service.RegistrationIntentService
import org.foundation101.karatel.utils.JobUtils
import javax.inject.Inject

class RegistrationRetryJob: Job() {
    companion object {
        const val TAG = "RegistrationRetryJob"
    }

    @Inject internal lateinit var preferences: KaratelPreferences

    override fun onRunJob(params: Params): Result {
        KaratelApplication.dagger().inject(this)

        return try {
            val token = RegistrationIntentService.obtainGCMToken()
            if (token != preferences.pushToken()) {
                preferences.setNewPushToken(token)
                JobUtils.schedule(TokenExchangeJob.TAG)
            }
            Job.Result.SUCCESS
        } catch (e: Exception) {
            Log.d(TAG, "Failed to complete token refresh", e)
            Mint.logException(e)
            Job.Result.RESCHEDULE
        }
    }
}