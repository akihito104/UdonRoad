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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.AppBarLayout.OnOffsetChangedListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.TweetInputFragment.TweetSendable;
import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
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
public class UserInfoActivity extends AppCompatActivity implements TweetSendable {
  public static final String TAG = UserInfoActivity.class.getSimpleName();
  private UserInfoPagerFragment viewPager;
  private ActivityUserInfoBinding binding;
  @Inject
  StatusCache statusCache;
  private UserInfoFragment userInfoAppbarFragment;
  private TweetInputFragment tweetInputFragment;
  private Map<Direction, UserAction> actionMap = new HashMap<>();
  private FlingableFABHelper flingableFABHelper;
  @Inject
  TwitterApi twitterApi;
  private UserSubscriber<StatusCache> userSubscriber;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);
    InjectionUtil.getComponent(this).inject(this);

    binding.ffab.hide();

    long userId = parseIntent();
    setUpAppbar();
    setUpUserInfoView(userId);

    viewPager = (UserInfoPagerFragment) getSupportFragmentManager()
        .findFragmentById(R.id.userInfo_pagerFragment);
    viewPager.setTabLayout(binding.userInfoTabs);
    flingableFABHelper = new FlingableFABHelper(binding.fabIndicator, binding.ffab);
    viewPager.setFABHelper(flingableFABHelper);
    viewPager.setUser(userId);
    userSubscriber = new UserSubscriber<>(twitterApi, statusCache,
        new FeedbackSubscriber.SnackbarFeedback(viewPager.getView()));
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
    statusCache.open(getApplicationContext());
    long userId = parseIntent();
    final User user = statusCache.findUser(userId);
    UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);

    setSupportActionBar(binding.userInfoToolbar);
    setupActionMap();
    UserAction.setupFlingableFAB(flingableFABHelper, actionMap, getApplicationContext());
  }

  @Override
  protected void onStop() {
    flingableFABHelper.getFab().setOnFlingListener(null);
    actionMap.clear();
    binding.userInfoToolbarTitle.setText("");
    binding.userInfoCollapsingToolbar.setTitleEnabled(false);
    binding.userInfoTabs.removeAllTabs();
    statusCache.close();
    closeTwitterInputView();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  private MenuItem replyCloseMenuItem;
  private MenuItem followingMenuItem;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Log.d(TAG, "onCreateOptionsMenu: ");
    getMenuInflater().inflate(R.menu.user_info, menu);
    followingMenuItem = menu.findItem(R.id.userInfo_following);
    replyCloseMenuItem = menu.findItem(R.id.userInfo_reply_close);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    Log.d(TAG, "onPrepareOptionsMenu: ");
    setupMenuVisibility();
    return super.onPrepareOptionsMenu(menu);
  }

  private void setupMenuVisibility() {
    replyCloseMenuItem.setVisible(tweetInputFragment != null);
    followingMenuItem.setVisible(!replyCloseMenuItem.isVisible());
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    switch (itemId) {
      case R.id.userInfo_heading:
        viewPager.scrollToTop();
        break;
      case R.id.userInfo_follow:
        userSubscriber.createFriendship(parseIntent());
        break;
      case R.id.userInfo_remove:
        userSubscriber.destroyFriendship(parseIntent());
        break;
      case R.id.userInfo_block_retweet:
        // todo
        break;
      case R.id.userInfo_mute:
        userSubscriber.createMute(parseIntent());
        break;
      case R.id.userInfo_block:
        userSubscriber.createBlock(parseIntent());
        break;
      case R.id.userInfo_r4s:
        userSubscriber.reportSpam(parseIntent());
        break;
      case R.id.userInfo_reply_close:
        closeTwitterInputView();
        break;
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
        String.format(resources.getString(R.string.tweet_name), user.getScreenName()));
  }

  private void showTwitterInputview(@TweetType int type, long statusId) {
    binding.userInfoAppbarContainer.setPadding(0, binding.userInfoToolbar.getHeight(), 0, 0);

    tweetInputFragment = TweetInputFragment.create(type, statusId);
    tweetInputFragment.setTweetSendFab(binding.userInfoTweetSend);
    getSupportFragmentManager().beginTransaction()
        .hide(userInfoAppbarFragment)
        .add(R.id.userInfo_appbar_container, tweetInputFragment)
        .commit();
    if (type == TYPE_REPLY) {
      binding.userInfoToolbar.setTitle("返信する");
    } else if (type == TYPE_QUOTE) {
      binding.userInfoToolbar.setTitle("コメントする");
    }
    binding.userInfoToolbarTitle.setVisibility(View.GONE);
    binding.userInfoAppbarLayout.setExpanded(true);
    setupMenuVisibility();
  }

  private void closeTwitterInputView() {
    if (tweetInputFragment == null) {
      return;
    }
    binding.userInfoAppbarLayout.setExpanded(false);
    binding.userInfoAppbarContainer.setPadding(0, 0, 0, 0);
    tweetInputFragment.collapseStatusInputView();
    tweetInputFragment.setTweetSendFab(null);
    getSupportFragmentManager().beginTransaction()
        .remove(tweetInputFragment)
        .show(userInfoAppbarFragment)
        .commit();
    binding.userInfoToolbar.setTitle("");
    binding.userInfoToolbarTitle.setVisibility(View.VISIBLE);
    tweetInputFragment = null;
    setupMenuVisibility();
  }

  @Override
  public void onBackPressed() {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
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
    showTwitterInputview(type, statusId);
  }

  @Override
  public void observeUpdateStatus(Observable<Status> updateStatusObservable) {
    updateStatusObservable.subscribe(
        new Action1<Status>() {
          @Override
          public void call(Status status) {
            closeTwitterInputView();
          }
        },
        new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Toast.makeText(getApplicationContext(), "send tweet: failure...", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "send tweet; ", throwable);
          }
        });
  }

  private void setupActionMap() {
    actionMap.put(Direction.UP, new UserAction(ActionResource.FAV, new Runnable() {
      @Override
      public void run() {
        viewPager.createFavorite();
      }
    }));
    actionMap.put(Direction.RIGHT, new UserAction(ActionResource.RETWEET, new Runnable() {
      @Override
      public void run() {
        viewPager.retweetStatus();
      }
    }));
    actionMap.put(Direction.UP_RIGHT, new UserAction());
    actionMap.put(Direction.DOWN, new UserAction(ActionResource.REPLY, new Runnable() {
      @Override
      public void run() {
        final long selectedTweetId = viewPager.getCurrentSelectedStatusId();
        showTwitterInputview(TYPE_REPLY, selectedTweetId);
      }
    }));
    actionMap.put(Direction.DOWN_RIGHT, new UserAction(ActionResource.QUOTE, new Runnable() {
      @Override
      public void run() {
        final long selectedTweetId = viewPager.getCurrentSelectedStatusId();
        showTwitterInputview(TYPE_QUOTE, selectedTweetId);
      }
    }));
  }
}
