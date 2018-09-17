package org.foundation101.karatel

object Const {
    private val apiSuffix = if (BuildConfig.DEBUG) "test" else "api"

    @JvmField
    val SERVER_URL = "https://karatel-$apiSuffix.foundation101.org/api/v1/"//-api -test
}