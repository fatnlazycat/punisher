package org.foundation101.karatel

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.foundation101.karatel.TestUtils.checkViewArrayIsDisplayed
import org.foundation101.karatel.TestUtils.checkViewArrayIsDisplayedWithScroll
import org.foundation101.karatel.activity.TipsActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TipsActivity_Test {
    @Rule
    @JvmField
    val testRule = ActivityTestRule<TipsActivity>(TipsActivity::class.java)

    @Test
    fun checkScreen() {
        checkViewArrayIsDisplayedWithScroll(arrayOf(R.id.imageView, R.id.editTextLoginEmail,
                R.id.editTextLoginPassword, R.id.forgotPasswButton, R.id.loginButton,
                R.id.aboLayout, R.id.facebookLoginButton, R.id.signUpButton))
    }

    @Test
    fun openSignUp() {
        onView(withId(R.id.signUpButton)).perform(click())
        checkViewArrayIsDisplayedWithScroll(arrayOf(R.id.toolbar, R.id.etSignUpEmail,
                R.id.etSignUpPassword, R.id.etSignUpSurname, R.id.etSignUpName,
                R.id.etSignUpSecondName, R.id.etSignUpPhone, R.id.checkBoxPersonalDataAgreement,
                R.id.buttonRegister))
    }

    @Test
    fun openForgotPassword() {
        onView(withId(R.id.forgotPasswButton)).perform(click())
        checkViewArrayIsDisplayed(arrayOf(R.id.llActivityForgotPassword,R.id.toolbarForgotPassword,
                R.id.imageViewForgotPassword, R.id.textViewForgotPassword, R.id.editTextForgotPasswordEmail,
                R.id.buttonForgotPassword))
    }
}