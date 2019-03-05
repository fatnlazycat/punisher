package org.foundation101.karatel

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onData
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.DrawerActions
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.foundation101.karatel.TestUtils.checkChildViewArrayIsDisplayedWithScroll
import org.foundation101.karatel.TestUtils.checkViewArrayIsDisplayed
import org.foundation101.karatel.TestUtils.checkViewArrayIsDisplayedWithScroll
import org.foundation101.karatel.activity.MainActivity
import org.foundation101.karatel.entity.Violation
import org.foundation101.karatel.manager.KaratelPreferences
import org.hamcrest.Matchers.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)

class MainActivity_Test {
    @Rule @JvmField
    val testRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    private fun getDrawerItemTitle(i: Int) = KaratelApplication.getInstance().resources.getStringArray(R.array.drawerMenuItems)[i]
    private fun openDrawer() = onView(withId(R.id.drawerLayoutMainActivity)).perform(DrawerActions.open())

    @Test fun openProfile() {
        val webServer = MockWebServer()
        webServer.start(8080)
        webServer.setDispatcher(MyRequestDispatcher())

        openDrawer()
        onView(withId(R.id.drawerHeaderLayout)).perform(click())

        val views = arrayOf(R.id.avatarProfileImageView, R.id.userNameTextView,
                R.id.profile_surname, R.id.profile_name, R.id.profile_second_name, R.id.profile_phone,
                R.id.llEmail, R.id.profile_email, R.id.profile_password)
        checkViewArrayIsDisplayedWithScroll(views)

        onView(
            withChild(
                withText(R.string.email)
            )
        ).check(
            matches(
                withChild(
                    withText(TestUser.EMAIL)
                )
            )
        )

        webServer.shutdown()
    }

    @Test fun openFragmentsFromDrawer() {
        //keys - fragment numbers, values - array of views that should be displayed
        val data = mapOf(
            0 to arrayOf(R.id.llFragmentMain, R.id.gridViewMain),
            1 to arrayOf(R.id.rlListOfRequests),
            2 to arrayOf(R.id.llComplainsBook, R.id.lvComplainTypes),
            3 to arrayOf(R.id.lvVideos),
            5 to arrayOf(R.id.svFragmentAbout, R.id.imageView2, R.id.textView3, R.id.textView5, R.id.button),
            6 to arrayOf(R.id.svFragmentSponsors, R.id.imageView2),
            7 to arrayOf(R.id.svFragmentPartners),
            8 to arrayOf(R.id.listViewNews),
            9 to arrayOf(R.id.llFragmentContacts, R.id.imageView2, R.id.textView3, R.id.btnFBMessenger,
                    R.id.btnTelegramBot, R.id.buttonEmail, R.id.weInSocialsTextView, R.id.buttonFacebook,
                    R.id.buttonInstagram, R.id.buttonYouTube, R.id.buttonTwitter, R.id.buttonTelegram)
        )

        data.asSequence()
            .map {
                openDrawer()

                onData(`is`(getDrawerItemTitle(it.key)))
                        .inAdapterView(withId(R.id.drawerListView))
                        .perform(click())

                it.value
            }.forEach { checkViewArrayIsDisplayed(it) }
    }

    @Test fun openWebPageFromDrawer() {
        openDrawer()
        onData(`is`("Підтримати проект"))
                .inAdapterView(withId(R.id.drawerListView))
                .perform(click())

        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        //uncheck 'set default app' button
        val chBoxRememberAction = device.findObject(UiSelector().text("Запам'ятати вибір"))
        with(chBoxRememberAction) {
            if (exists() && isChecked) click()
        }

        fun clickChromeButton(): Boolean {
            val btnChrome = device.findObject(UiSelector().text("Chrome"))
            return (btnChrome.exists() && btnChrome.clickAndWaitForNewWindow())
        }

        fun performCheck() {
            val urlBar = device.findObject(UiSelector().textMatches("(https://)?secure.wayforpay.com/page"))
            assertTrue(urlBar.waitForExists(TimeUnit.SECONDS.toMillis(5)))
        }

        if (clickChromeButton()) {
            performCheck()
        } else {
            val btnMore = device.findObject(UiSelector().text("Більше"))
            if (btnMore.exists())btnMore.click()
            clickChromeButton()
            performCheck()
        }

    }

    @Test fun openYouTubeActivity() {
        openDrawer()
        onData(`is`("Відеонавчання"))
                .inAdapterView(withId(R.id.drawerListView))
                .perform(click())

        onData(hasEntry("header", "Інформація з обмеженим доступом"))
                .inAdapterView(withId(R.id.lvVideos))
                .perform(click())

        checkViewArrayIsDisplayed(arrayOf(R.id.toolbar, R.id.youTubeView, R.id.tvTitle, R.id.tvDescription))
    }

    @Test fun openViolationActivity() {
        onData(instanceOf(Violation::class.java))
                .inAdapterView(withId(R.id.gridViewMain))
                .atPosition(0)
                .perform(click())

        checkViewArrayIsDisplayed(arrayOf(R.id.toolbar, R.id.tabInfo))
        checkViewArrayIsDisplayedWithScroll(arrayOf(
                R.id.textViewViolationHeader, R.id.llAddEvidence, R.id.punishButton, R.id.saveButton
        ))

        onView(withId(R.id.requisitesList)).check(matches(isDisplayingAtLeast(1)))

        checkChildViewArrayIsDisplayedWithScroll(arrayOf(
                R.id.uploadButton, R.id.addedPhotoVideoTextView, R.id.imageButtonAddEvidence))
    }


    companion object {
        private const val TEST_SESSION_TOKEN = "testSessionToken"

        @JvmStatic
        @BeforeClass
        fun initialize() {
            val oldSessionToken = KaratelPreferences.sessionToken()
            if (oldSessionToken.isEmpty()) KaratelPreferences.setSessionToken(TEST_SESSION_TOKEN)
        }

        @JvmStatic
        @AfterClass
        fun finalize() {
            if (TEST_SESSION_TOKEN == KaratelPreferences.sessionToken()) KaratelPreferences.remove(Globals.SESSION_TOKEN)
        }
    }
}

object TestUser {
    const val EMAIL = "dmitry.kosiakov@aejis.eu"
}

internal class MyRequestDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {

        return when (request.path){
            "/users/" + KaratelPreferences.userId() -> MockResponse().setResponseCode(200).setBody(userJSON)
            else -> MockResponse().setResponseCode(404)
        }

    }
    private val userJSON: String = JSONObject(mapOf(
        "status"         to "success",
        "data"           to JSONObject(mapOf(
                            "id"      to 44,
                            "surname" to "Тестовое",
                            "avatar"  to JSONObject(mapOf(
                                "url"   to "/uploads/user/avatar/44/filesavatar44.png",
                                "thumb" to JSONObject(mapOf("url" to "/uploads/user/avatar/44/thumb_filesavatar44.png"))
                            )),
        "firstname"      to "Имя",
        "secondname"     to "Отчествович",
        "phone_number"   to "+380958742405",
        "email"          to TestUser.EMAIL,
        "status"         to "active",
        "admin"          to false,
        "user_status_id" to null,
        "created_at"     to "2016-06-22T17:30:42.675+03:00",
        "updated_at"     to "2018-09-14T12:48:24.584+03:00",
        "banned_message" to "Вас забанено-с..."
        ))
    )).toString()
}