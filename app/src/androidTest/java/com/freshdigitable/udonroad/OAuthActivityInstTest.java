package com.freshdigitable.udonroad;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
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
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;

import java.util.Collection;

import javax.inject.Inject;

import twitter4j.Twitter;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2017/04/20.
 */
@RunWith(Enclosed.class)
public class OAuthActivityInstTest {
  public static class WhenLaunch extends Base {
    @Rule
    public final IntentsTestRule<MainActivity> rule
        = new IntentsTestRule<>(MainActivity.class, false, false);

    @Test
    public void launchMainActivityWithNoAccessToken_then_OAuthActivityIsLaunched() {
      rule.launchActivity(new Intent());
      onView(withId(R.id.button_oauth)).check(matches(isDisplayed()));
    }

    @Override
    void tearDownActivity() throws Exception {
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        final Activity oAuthActivity = findResumeActivityByClass(OAuthActivity.class);
        assertThat(oAuthActivity, is(notNullValue()));
        oAuthActivity.finish();
      });
      Thread.sleep(800);
    }
  }

  public static class WhenResume extends Base {
    @Rule
    public final IntentsTestRule<OAuthActivity> rule
        = new IntentsTestRule<>(OAuthActivity.class, false, false);

    @Test
    public void resumeWithValidToken() throws Exception {
      final Intent launch = OAuthActivity.createIntent(InstrumentationRegistry.getTargetContext(), MainActivity.class);
      rule.launchActivity(launch);
      showHome();

      final String callbackUrl = InstrumentationRegistry.getTargetContext().getString(R.string.callback_url);
      final Uri data = Uri.parse(callbackUrl + "?oauth_verifier=valid.token");
      assertThat(data.getQueryParameter("oauth_verifier"), is("valid.token"));
      final Intent fromBrowser = new Intent(Intent.ACTION_VIEW, data);
      fromBrowser.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      rule.getActivity().startActivity(fromBrowser);
      Thread.sleep(500);

      try{
        appSetting.open();
        assertThat(appSetting.getCurrentUserAccessToken().getToken(), is("valid.token"));
        assertThat(appSetting.getCurrentUserAccessToken().getTokenSecret(), is("valid.secret"));
        assertThat(appSetting.getCurrentUserId(), is(100L));
      } finally {
        appSetting.close();
      }
      intended(hasComponent(MainActivity.class.getName()));

      try {
        Espresso.registerIdlingResources(idlingResource);
        onView(withText("Home")).check(matches(isDisplayed()));
      } finally {
        Espresso.unregisterIdlingResources(idlingResource);
      }
    }

    @Before
    public void setup() throws Exception {
      super.setup();
      final AccessToken accessToken = mock(AccessToken.class);
      when(accessToken.getUserId()).thenReturn(100L);
      when(accessToken.getToken()).thenReturn("valid.token");
      when(accessToken.getTokenSecret()).thenReturn("valid.secret");
      when(twitter.getOAuthAccessToken(ArgumentMatchers.<RequestToken>any(), eq("valid.token")))
          .thenReturn(accessToken);
    }

    @Override
    void tearDownActivity() throws Exception {
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        final Activity activity = findResumeActivityByClass(MainActivity.class);
        assertThat(activity, is(notNullValue()));
        activity.finish();
      });
      Thread.sleep(1000);
    }

    private static void showHome() throws InterruptedException {
      Intent home = new Intent();
      home.setAction(Intent.ACTION_MAIN);
      home.addCategory(Intent.CATEGORY_HOME);
      home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      InstrumentationRegistry.getTargetContext().startActivity(home);
      Thread.sleep(500);
    }

    private final IdlingResource idlingResource = new IdlingResource() {
      @Override
      public String getName() {
        return "launchMain";
      }

      @Override
      public boolean isIdleNow() {
        return findResumeActivityByClass(MainActivity.class) != null;
      }

      @Override
      public void registerIdleTransitionCallback(ResourceCallback callback) {}
    };
  }

  static abstract class Base {
    @Inject
    AppSettingStore appSetting;
    @Inject
    Twitter twitter;

    @Before
    @CallSuper
    public void setup() throws Exception {
      StorageUtil.initStorage();
      TestInjectionUtil.getComponent().inject(this);
      appSetting.open();
      appSetting.clear();
      appSetting.close();
      when(twitter.getAPIConfiguration()).thenThrow(IllegalStateException.class);
      when(twitter.getId()).thenThrow(IllegalStateException.class);
      when(twitter.showUser(anyLong())).thenThrow(IllegalStateException.class);
      when(twitter.verifyCredentials()).thenThrow(IllegalStateException.class);
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
      reset(twitter);
      tearDownActivity();
      StorageUtil.checkAllRealmInstanceCleared();
    }

    abstract void tearDownActivity() throws Exception;
  }

  private static Activity findResumeActivityByClass(Class<? extends Activity> clz) {
    final Collection<Activity> resumeActivities
        = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
    for (Activity activity : resumeActivities) {
      if (activity.getClass().isAssignableFrom(clz)) {
        return activity;
      }
    }
    return null;
  }
}
