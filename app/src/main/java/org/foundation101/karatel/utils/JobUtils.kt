package org.foundation101.karatel.utils

import android.util.Log
import com.evernote.android.job.JobRequest
import org.foundation101.karatel.manager.KaratelPreferences
import java.util.concurrent.TimeUnit

object JobUtils {
    const val TAG = "JobUtils"

    fun schedule(tag: String) {
        val jobRequest = JobRequest.Builder(tag)
                //start in 5 min (do not start immediately - race conditions with other code!),
                //end after 10 min
                .setExecutionWindow(TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(10))
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(true)
                .setUpdateCurrent(true)
                .build()

        KaratelPreferences.setPendingJob(makeJobId(tag, jobRequest.jobId))
        jobRequest.scheduleAsync()
        Log.d(TAG, "scheduled $tag  id=${jobRequest.jobId}")
    }

    fun pendingJob(): Pair<String, Int>? {
        return jobData(KaratelPreferences.pendingJob())
    }

    fun pendingJobTag(): String = pendingJob()?.first ?: ""

    fun makeJobId(jobTag: String, id: Int): String = "$jobTag $id"

    private fun jobData(jobDataString: String): Pair<String, Int>? {
        val matchedList = Regex("(\\w+) (\\d+)").find(jobDataString)?.groupValues
        matchedList?.let {
            if (it.size > 2) return Pair(it[1], it[2].toInt())
        }
        return null
    }
}