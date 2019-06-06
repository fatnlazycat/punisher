package org.foundation101.karatel.utils

import org.foundation101.karatel.Globals
import org.foundation101.karatel.KaratelApplication
import org.foundation101.karatel.manager.CameraManager
import org.foundation101.karatel.manager.KaratelPreferences
import java.io.File

object FileUtils {
    fun avatarFileName(temporary: Boolean = false): String {
        val temp = if (temporary) "temp" else ""
        return "" + KaratelApplication.getInstance().filesDir + "avatar" + KaratelPreferences().userId() + temp + CameraManager.PNG
    }

    fun swapRename(name1: String, name2: String): Boolean {
        val file1 = File(name1)
        val file2 = File(name2)

        val tempFile = File(file1.parentFile, "tmpAvatar" + CameraManager.PNG)
        if (tempFile.exists()) tempFile.delete()

        return if (file2.exists())
             file1   .renameTo(tempFile) &&
             file2   .renameTo(file1)    &&
             tempFile.renameTo(file2)
        else file1   .renameTo(file2)
    }
}