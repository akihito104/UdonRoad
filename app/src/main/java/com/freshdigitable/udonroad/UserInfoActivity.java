/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.UserInfoPagerFragment.UserPageInfo;
import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB.OnIffabItemSelectedListener;
import com.freshdigitable.udonroad.module.InjectionUtil;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.User;

import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.TweetInputFragment.TYPE_REPLY;
import static com.freshdigitable.udonroad.TweetInputFragment.TweetType;

/**
 * UserInfoActivity shows information and tweets of specified user.
 *
 * Created by akihit on 2016/01/30.
 */
public class UserInfoActivity extends AppCompatActivity
    implements TweetSendable, FabHandleable {
  public static final String TAG = UserInfoActivity.class.getSimpleName();
  private UserInfoPagerFragment viewPager;
  private ActivityUserInfoBinding binding;
  @Inject
  TypedCache<User> userCache;
  private UserInfoFragment userInfoAppbarFragment;
  private TweetInputFragment tweetInputFragment;
  private Subscription subscription;
  private StatusDetailFragment statusDetailFragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);
    InjectionUtil.getComponent(this).inject(this);

    binding.userInfoIffab.hide();

    long userId = parseIntent();
    setUpAppbar();
    setUpUserInfoView(userId);

    viewPager = UserInfoPagerFragment.create(userId);
    getSupportFragmentManager().beginTransaction()
        .add(binding.userInfoTimelineContainer.getId(), viewPager)
        .commit();
  }

  private void setUpUserInfoView(long userId) {
    userInfoAppbarFragment = UserInfoFragment.create(userId);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.userInfo_appbar_container, userInfoAppbarFragment)
        .commit();
  }

  private void setUpAppbar() {
    binding.userInfoToolbar.setTitle("");

    final TextView toolbarTitle = binding.userInfoToolbarTitle;
    binding.userInfoAppbarLayout.addOnOffsetChangedListener(new OnOffsetChangedListener() {
      private boolean isTitleVisible = toolbarTitle.getVisibility() == View.VISIBLE;

      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final int totalScrollRange = appBarLayout.getTotalScrollRange();
        final float percent = (float) Math.abs(verticalOffset) / (float) totalScrollRange;
        if (percent > 0.9) {
          if (!isTitleVisible) {
            startAnimation(toolbarTitle, View.VISIBLE);
            isTitleVisible = true;
          }
        } else {
          if (isTitleVisible) {
            startAnimation(toolbarTitle, View.INVISIBLE);
            isTitleVisible = false;
          }
        }
      }

      private void startAnimation(View v, int visibility) {
        AlphaAnimation animation = (visibility == View.VISIBLE)
            ? new AlphaAnimation(0f, 1f)
            : new AlphaAnimation(1f, 0f);
        animation.setDuration(200);
        animation.setFillAfter(true);
        v.startAnimation(animation);
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    userCache.open();
    long userId = parseIntent();
    final User user = userCache.find(userId);
    UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);

    binding.userInfoTabs.setupWithViewPager(viewPager.getViewPager());
    subscription = userCache.observeById(userId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            for (UserPageInfo p : UserPageInfo.values()) {
              final TabLayout.Tab tab = binding.userInfoTabs.getTabAt(p.ordinal());
              if (tab != null) {
                tab.setText(p.createTitle(user));
              }
            }
          }
        });

    setSupportActionBar(binding.userInfoToolbar);
    setupActionMap();
  }

  @Override
  protected void onStop() {
    super.onStop();
    binding.userInfoIffab.setOnIffabItemSelectedListener(null);
    binding.userInfoToolbarTitle.setText("");
    binding.userInfoCollapsingToolbar.setTitleEnabled(false);
    binding.userInfoTabs.removeAllTabs();
    binding.userInfoTabs.setupWithViewPager(null);
    subscription.unsubscribe();
    userCache.close();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_cancel) {
      closeTwitterInputView();
    }
    return super.onOptionsItemSelected(item);
  }

  public static Intent createIntent(Context context, User user) {
    return createIntent(context, user.getId());
  }

  public static Intent createIntent(Context context, long userId) {
    Intent intent = new Intent(context, UserInfoActivity.class);
    intent.putExtra("user", userId);
    return intent;
  }

  public static void start(Context context, long userId) {
    final Intent intent = createIntent(context, userId);
    context.startActivity(intent);
  }

  public static void start(Activity activity, User user, View userIcon) {
    final Intent intent = createIntent(activity.getApplicationContext(), user.getId());
    ViewCompat.setTransitionName(userIcon, "user_icon");
    ActivityCompat.startActivity(activity, intent,
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, userIcon, "user_icon").toBundle());
  }

  private long parseIntent() {
    return getIntent().getLongExtra("user", -1L);
  }

  public static void bindUserScreenName(TextView textView, User user) {
    final Resources resources = textView.getContext().getResources();
    textView.setText(
        String.format(resources.getString(R.string.tweet_screenName), user.getScreenName()));
  }

  private void showTwitterInputView(@TweetType int type, long statusId) {
    binding.userInfoAppbarContainer.setPadding(0, binding.userInfoToolbar.getHeight(), 0, 0);

    tweetInputFragment = TweetInputFragment.create(type, statusId);
    tweetInputFragment.setTweetSendFab(binding.userInfoTweetSend);
    getSupportFragmentManager().beginTransaction()
        .hide(userInfoAppbarFragment)
        .add(R.id.userInfo_appbar_container, tweetInputFragment)
        .commit();
    binding.userInfoToolbarTitle.setVisibility(View.GONE);
    if (type == TYPE_REPLY) {
      binding.userInfoToolbar.setTitle("返信する");
    } else if (type == TYPE_QUOTE) {
      binding.userInfoToolbar.setTitle("コメントする");
    }
    binding.userInfoAppbarLayout.setExpanded(true);
  }

  private void closeTwitterInputView() {
    if (tweetInputFragment == null) {
      return;
    }
    binding.userInfoAppbarLayout.setExpanded(false);
    binding.userInfoAppbarContainer.setPadding(0, 0, 0, 0);
    getSupportFragmentManager().beginTransaction()
        .remove(tweetInputFragment)
        .show(userInfoAppbarFragment)
        .commit();
    binding.userInfoToolbar.setTitle("");
    binding.userInfoToolbarTitle.setVisibility(View.VISIBLE);
    tweetInputFragment = null;
  }

  @Override
  public void onBackPressed() {
    if (statusDetailFragment != null && statusDetailFragment.isVisible()) {
      closeStatusDetail();
      return;
    }
    if (binding.userInfoIffab.getVisibility() == View.VISIBLE) {
      viewPager.clearSelectedTweet();  // it also hides ffab.
      return;
    }
    if (tweetInputFragment != null && tweetInputFragment.isVisible()) {
      closeTwitterInputView();
      return;
    }
    super.onBackPressed();
  }

  @Override
  public void setupInput(@TweetType int type, long statusId) {
    showTwitterInputView(type, statusId);
  }

  @Override
  public Observable<Status> observeUpdateStatus(Observable<Status> updateStatusObservable) {
    return updateStatusObservable.doOnNext(new Action1<Status>() {
      @Override
      public void call(Status status) {
        closeTwitterInputView();
      }
    });
  }

  private void setupActionMap() {
    binding.userInfoIffab.setOnIffabItemSelectedListener(new OnIffabItemSelectedListener() {
      @Override
      public void onItemSelected(@NonNull MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.iffabMenu_main_fav) {
          viewPager.createFavorite();
        } else if (itemId == R.id.iffabMenu_main_rt) {
          viewPager.retweetStatus();
        } else if (itemId == R.id.iffabMenu_main_favRt) {
          viewPager.createFavAndRetweet();
        } else if (itemId == R.id.iffabMenu_main_reply) {
          final long selectedTweetId = viewPager.getCurrentSelectedStatusId();
          showTwitterInputView(TYPE_REPLY, selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_quote) {
          final long selectedTweetId = viewPager.getCurrentSelectedStatusId();
          showTwitterInputView(TYPE_QUOTE, selectedTweetId);
        } else if (itemId == R.id.iffabMenu_main_detail) {
          final long statusId = viewPager.getCurrentSelectedStatusId();
          showStatusDetail(statusId);
        }
      }
    });
  }

  private void showStatusDetail(long statusId) {
    binding.userInfoIffab.hide();
    statusDetailFragment = StatusDetailFragment.getInstance(statusId);
    getSupportFragmentManager().beginTransaction()
        .hide(viewPager)
        .add(binding.userInfoTimelineContainer.getId(), statusDetailFragment)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        .commit();
  }

  private void closeStatusDetail() {
    getSupportFragmentManager().beginTransaction()
        .remove(statusDetailFragment)
        .show(viewPager)
        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE)
        .commit();
    binding.userInfoIffab.show();
  }

  @Override
  public void showFab() {
    final UserPageInfo currentPage = viewPager.getCurrentPage();
    if (currentPage.isStatus()) {
      binding.userInfoIffab.show();
    }
  }

  @Override
  public void hideFab() {
    binding.userInfoIffab.hide();
  }
}
