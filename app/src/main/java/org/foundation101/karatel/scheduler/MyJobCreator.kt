package org.foundation101.karatel.scheduler

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

class MyJobCreator: JobCreator {
    override fun create(tag: String): Job? = when (tag) {
        TokenExchangeJob.TAG -> TokenExchangeJob()
        FetchRequestsJob.TAG -> FetchRequestsJob()
        //RegistrationRetryJob.TAG -> RegistrationRetryJob()
        else                 -> null
    }
}