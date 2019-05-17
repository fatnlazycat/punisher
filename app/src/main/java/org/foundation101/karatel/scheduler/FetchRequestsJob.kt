package org.foundation101.karatel.scheduler

import com.evernote.android.job.Job
import org.foundation101.karatel.KaratelApplication
import org.foundation101.karatel.asyncTasks.ErrorHandler
import org.foundation101.karatel.asyncTasks.RequestListFetcher
import java.util.concurrent.CopyOnWriteArrayList

class FetchRequestsJob: Job() {
    companion object {
        const val TAG = "FetchRequestsJob"
    }

    override fun onRunJob(params: Params): Result {
        val result = RequestListFetcher.parseResponse(
                RequestListFetcher.getRequests(),
                object: ErrorHandler() { override fun handleError(message: Any, e: Exception?) {} }
        )
        return if (result != null) {
            KaratelApplication.getInstance().requests = CopyOnWriteArrayList(result)
            Result.SUCCESS
        } else Result.RESCHEDULE
    }
}