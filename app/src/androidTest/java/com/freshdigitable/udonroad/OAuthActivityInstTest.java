package com.freshdigitable.udonroad;

import android.content.Intent;
import android.support.test.espresso.intent.rule.IntentsTestRule;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

import twitter4j.Twitter;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by akihit on 2017/04/20.
 */

public class OAuthActivityInstTest {
  @Rule
  public IntentsTestRule<MainActivity> rule
      = new IntentsTestRule<>(MainActivity.class, false, false);

  @Inject
  AppSettingStore appSetting;
  @Inject
  Twitter twitter;

  @Before
  public void setup() {
    StorageUtil.initStorage();
    TestInjectionUtil.getComponent().inject(this);
    appSetting.open();
    appSetting.clear();
    appSetting.close();
  }

  @Test
  public void launchOAuthActivity () {
    rule.launchActivity(new Intent());
    onView(withId(R.id.button_oauth)).check(matches(isDisplayed()));
  }

//  @Test
//  public void startOAuth() throws TwitterException {
//    final RequestToken requestToken = new RequestToken("token", "secret");
//    when(twitter.getOAuthRequestToken(Mockito.any())).thenReturn(requestToken);
//
//    rule.launchActivity(new Intent());
//
//    onView(withId(R.id.button_oauth)).perform(click());
//    intended(hasAction(Intent.ACTION_VIEW));
//  }
}
