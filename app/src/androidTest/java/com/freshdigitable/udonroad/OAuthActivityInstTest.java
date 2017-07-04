package com.freshdigitable.udonroad;

import android.app.Activity;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.util.StorageUtil;
import com.freshdigitable.udonroad.util.TestInjectionUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collection;

import javax.inject.Inject;

import twitter4j.Twitter;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

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
  public void setup() throws Exception {
    StorageUtil.initStorage();
    TestInjectionUtil.getComponent().inject(this);
    appSetting.open();
    appSetting.clear();
    appSetting.close();
    when(twitter.getAPIConfiguration()).thenThrow(IllegalStateException.class);
    when(twitter.getId()).thenThrow(IllegalStateException.class);
    when(twitter.verifyCredentials()).thenThrow(IllegalStateException.class);
  }

  @After
  public void tearDown() throws Exception {
    reset(twitter);
    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
      final Collection<Activity> activities = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
      assertThat(activities.size(), is(not(0)));
      final Activity oAuthActivity = findOAuthActivity(activities);
      assertThat(oAuthActivity, is(not(nullValue())));
      oAuthActivity.finish();
    });
    Thread.sleep(800);
    StorageUtil.checkAllRealmInstanceCleared();
  }

  private static Activity findOAuthActivity(Collection<Activity> activities) {
    for (Activity activity : activities) {
      if (activity instanceof OAuthActivity) {
        return activity;
      }
    }
    return null;
  }

  @Test
  public void launchMainActivityWithNoAccessToken_then_OAuthActivityIsLaunched () {
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
