package org.foundation101.karatel.utils

import android.util.Base64

object TextUtils {
    fun encodeStringWithSHA(string: String): String =
        Base64.encodeToString(string.toByteArray(Charsets.UTF_8), Base64.DEFAULT)

    fun decodeString(string: String): String =
        String(Base64.decode(string, Base64.DEFAULT), Charsets.UTF_8)
}