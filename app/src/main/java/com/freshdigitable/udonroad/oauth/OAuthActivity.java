/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.freshdigitable.udonroad.oauth;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.freshdigitable.udonroad.FabViewModel;
import com.freshdigitable.udonroad.MainActivity;
import com.freshdigitable.udonroad.MainApplication;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.SnackbarCapable;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.timeline.TimelineFragment;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static com.freshdigitable.udonroad.FabViewModel.Type.FAB;
import static com.freshdigitable.udonroad.FabViewModel.Type.HIDE;
import static com.freshdigitable.udonroad.FabViewModel.Type.TOOLBAR;

/**
 * OAuthActivity is for OAuth authentication with Twitter.
 *
 * Created by akihit on 15/10/22.
 */
public class OAuthActivity extends AppCompatActivity
    implements SnackbarCapable, OnUserIconClickedListener, HasSupportFragmentInjector {
  private static final String TAG = OAuthActivity.class.getName();

  @Inject
  TwitterApi twitterApi;
  @Inject
  AppSettingStore appSettings;
  @Inject
  PublishProcessor<UserFeedbackEvent> userFeedback;
  private IndicatableFFAB ffab;
  private Toolbar toolbar;
  private FabViewModel fabViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    ffab = findViewById(R.id.ffab);
    toolbar = findViewById(R.id.oauth_toolbar);
    setSupportActionBar(toolbar);

    if (savedInstanceState == null) {
      final DemoTimelineFragment demoTimelineFragment = new DemoTimelineFragment();
      final Bundle args = TimelineFragment.createArgs(StoreType.DEMO, -1, "");
      demoTimelineFragment.setArguments(args);
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.oauth_timeline_container, demoTimelineFragment)
          .commit();
    }
    fabViewModel = ViewModelProviders.of(this).get(FabViewModel.class);
    fabViewModel.getFabState().observe(this, type -> {
      if (type == FAB) {
        ffab.transToFAB();
      } else if (type == TOOLBAR) {
        ffab.transToToolbar();
      } else if (type == HIDE) {
        ffab.hide();
      } else {
        ffab.show();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    ffab.setOnIffabItemSelectedListener(fabViewModel::onMenuItemSelected);
    appSettings.open();
    if (appSettings.hasAuthenticatedUser()) {
      toolbar.setTitle(R.string.title_add_account);
    }
    appSettings.close();
  }

  @Override
  protected void onStop() {
    super.onStop();
    ffab.setOnIffabItemSelectedListener(null);
  }

  @Override
  public void onBackPressed() {
    appSettings.open();
    if (appSettings.hasAuthenticatedUser()) {
      startActivity(new Intent(this, getRedirect()));
    }
    appSettings.close();
    super.onBackPressed();
  }

  private static final String SS_REQUEST_TOKEN = "ss_requestToken";
  private RequestToken requestToken;

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(SS_REQUEST_TOKEN, requestToken);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    requestToken = (RequestToken) savedInstanceState.getSerializable(SS_REQUEST_TOKEN);
  }

  void startAuthorization() {
    ((MainApplication) getApplication()).logout();
    twitterApi.fetchOAuthRequestToken()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(err -> Timber.tag(TAG).e(err, "authentication error: "))
        .subscribe(this::startAuthAction,
            e -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_failed)));
  }

  private void startAuthAction(RequestToken token) {
    requestToken = token;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(token.getAuthorizationURL()));
    startActivity(intent);
  }

  void startAuthentication(final String verifier) {
    twitterApi.fetchOAuthAccessToken(requestToken, verifier)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(err -> Timber.tag(TAG).e(err, "authentication error: "))
        .subscribe(this::checkOAuth,
            e -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_failed)));
  }

  private void checkOAuth(AccessToken accessToken) {
    userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_success));
    appSettings.open();
    appSettings.storeAccessToken(accessToken);
    ((MainApplication) getApplication()).logout();
    appSettings.close();
    ((MainApplication) getApplication()).login(accessToken.getUserId());
    Intent intent = new Intent(this, getRedirect());
    startActivity(intent);
    finish();
  }

  private static final String EXTRAS_REDIRECT = "redirect";

  public static Intent createIntent(Activity redirect) {
    if (redirect instanceof OAuthActivity) {
      throw new IllegalArgumentException();
    }
    return createIntent(redirect.getApplicationContext(), redirect.getClass());
  }

  public static Intent createIntent(Context context, Class<? extends Activity> clz) {
    final Intent intent = new Intent(context, OAuthActivity.class);
    intent.putExtra(EXTRAS_REDIRECT, clz);
    return intent;
  }

  private Class<?> getRedirect() {
    final Bundle extras = getIntent().getExtras();
    return extras != null ? (Class<?>) extras.getSerializable(EXTRAS_REDIRECT)
        : MainActivity.class;
  }

  public static void start(Activity redirect) {
    final Intent intent = createIntent(redirect);
    redirect.startActivity(intent);
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.oauth_timeline_container);
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    this.userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_user_icon));
  }

  @Inject
  DispatchingAndroidInjector<Fragment> androidInjector;

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return androidInjector;
  }
}
