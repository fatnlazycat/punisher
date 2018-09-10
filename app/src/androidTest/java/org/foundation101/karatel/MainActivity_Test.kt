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
import org.foundation101.karatel.activity.MainActivity
import org.hamcrest.Matchers.`is`
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class MainActivity_Test {
    @Rule @JvmField
    val testRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    private fun getDrawerItemTitle(i: Int) = KaratelApplication.getInstance().resources.getStringArray(R.array.drawerMenuItems)[i]

    @Test
    fun openFragmentsFromDrawer() {

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
            onView(withId(R.id.drawerLayoutMainActivity)).perform(DrawerActions.open())

            onData(`is`(getDrawerItemTitle(it.key)))
                    .inAdapterView(withId(R.id.drawerListView))
                    .perform(click())

            it.value
        }
        .flatMap { it.asSequence() }
        .forEach {
            onView(withId(it)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun openWebPageFromDrawer() {
        onView(withId(R.id.drawerLayoutMainActivity)).perform(DrawerActions.open())
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
            val urlBar = device.findObject(UiSelector().text("https://secure.wayforpay.com/page"))
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
}