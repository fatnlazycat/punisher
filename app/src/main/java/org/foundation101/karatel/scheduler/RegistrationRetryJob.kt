package org.foundation101.karatel.scheduler

import android.util.Log
import com.evernote.android.job.Job
import com.splunk.mint.Mint
import org.foundation101.karatel.manager.KaratelPreferences
import org.foundation101.karatel.service.RegistrationIntentService
import org.foundation101.karatel.utils.JobUtils

class RegistrationRetryJob: Job() {
    companion object {
        const val TAG = "RegistrationRetryJob"
    }

    override fun onRunJob(params: Params): Result {
        return try {
            val token = RegistrationIntentService.obtainGCMToken()
            if (token != KaratelPreferences.pushToken()) {
                KaratelPreferences.setNewPushToken(token)
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