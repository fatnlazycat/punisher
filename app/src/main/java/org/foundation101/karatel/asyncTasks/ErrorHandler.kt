package org.foundation101.karatel.asyncTasks

import org.foundation101.karatel.Globals

open class ErrorHandler {
    open fun  handleError(message: Any, e: Exception?) {
        when (message) {
            is String -> if (e == null) Globals.showMessage(message)
                         else Globals.showError  (message, e)
            is Int    -> if (e == null) Globals.showMessage(message)
                         else Globals.showError  (message, e)
            else      -> {}
        }

    }
}