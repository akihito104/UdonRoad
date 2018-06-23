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

package com.freshdigitable.udonroad.user;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
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
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.AppViewModelProviderFactory;
import com.freshdigitable.udonroad.FabViewModel;
import com.freshdigitable.udonroad.OnSpanClickListener;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.SnackbarCapable;
import com.freshdigitable.udonroad.TimelineContainerSwitcher;
import com.freshdigitable.udonroad.TimelineContainerSwitcher.ContentType;
import com.freshdigitable.udonroad.ToolbarTweetInputToggle;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.input.TweetInputModel;
import com.freshdigitable.udonroad.input.TweetInputViewModel;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.timeline.TimelineFragment;

import javax.inject.Inject;

import dagger.android.AndroidInjection;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;
import twitter4j.User;

import static com.freshdigitable.udonroad.FabViewModel.Type.FAB;
import static com.freshdigitable.udonroad.FabViewModel.Type.HIDE;
import static com.freshdigitable.udonroad.FabViewModel.Type.TOOLBAR;
import static com.freshdigitable.udonroad.input.TweetInputFragment.TYPE_QUOTE;
import static com.freshdigitable.udonroad.input.TweetInputFragment.TYPE_REPLY;
import static com.freshdigitable.udonroad.input.TweetInputFragment.TweetType;

/**
 * UserInfoActivity shows information and tweets of specified user.
 *
 * Created by akihit on 2016/01/30.
 */
public class UserInfoActivity extends AppCompatActivity
    implements SnackbarCapable, OnUserIconClickedListener,
    OnSpanClickListener, TimelineFragment.OnItemClickedListener, HasSupportFragmentInjector {
  public static final String TAG = UserInfoActivity.class.getSimpleName();
  private UserInfoPagerFragment viewPager;
  private ActivityUserInfoBinding binding;
  @Inject
  TypedCache<User> userCache;
  private UserInfoFragment userInfoAppbarFragment;
  private TimelineContainerSwitcher timelineContainerSwitcher;
  private FabViewModel fabViewModel;
  private Disposable tweetUploadSubs;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AndroidInjection.inject(this);
    super.onCreate(savedInstanceState);
    if (savedInstanceState == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    final View v = findViewById(R.id.userInfo_appbar_container);
    if (v == null) {
      binding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);
    } else {
      binding = DataBindingUtil.findBinding(v);
    }

    setUpAppbar();
    setupInfoAppbarFragment(getUserId());

    fabViewModel = ViewModelProviders.of(this).get(FabViewModel.class);
    fabViewModel.getFabState().observe(this, type -> {
      if (type == FAB) {
        binding.ffab.transToFAB(timelineContainerSwitcher.isItemSelected() ?
            View.VISIBLE : View.INVISIBLE);
      } else if (type == TOOLBAR) {
        binding.ffab.transToToolbar();
      } else if (type == HIDE) {
        if (viewPager.getSelectedItemId() > 0) {
          return;
        }
        binding.ffab.hide();
      } else {
        binding.ffab.show();
      }
    });
    fabViewModel.getMenuState().observe(this, FabViewModel.createMenuStateObserver(binding.ffab));
  }

  private void setupInfoAppbarFragment(long userId) {
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
    final User user = userCache.find(getUserId());
    UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);
    setSupportActionBar(binding.userInfoToolbar);
    setupActionMap();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1
        || getWindow().getSharedElementEnterTransition() == null) {
      onEnterAnimationComplete();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    timelineContainerSwitcher.setOnContentChangedListener(null);
    binding.ffab.setOnIffabItemSelectedListener(null);
    binding.userInfoToolbarTitle.setText("");
    binding.userInfoCollapsingToolbar.setTitleEnabled(false);
    userCache.close();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    Utils.maybeDispose(tweetUploadSubs);
  }

  @Override
  public void onEnterAnimationComplete() {
    userInfoAppbarFragment.onEnterAnimationComplete();
    setupTimeline();
    final User user = userCache.find(getUserId());
    timelineContainerSwitcher.setOnContentChangedListener((type, title) -> {
      if (type == ContentType.MAIN) {
        UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);
        binding.userInfoTabs.setVisibility(View.VISIBLE);
      } else {
        binding.userInfoToolbarTitle.setText(title);
        binding.userInfoTabs.setVisibility(View.GONE);
      }
    });
    timelineContainerSwitcher.syncState();
  }

  private void setupTimeline() {
    final Fragment mainFragment = getSupportFragmentManager().findFragmentByTag(TimelineContainerSwitcher.MAIN_FRAGMENT_TAG);
    if (mainFragment == null) {
      viewPager = UserInfoPagerFragment.create(getUserId());
      viewPager.setTabLayout(binding.userInfoTabs);
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.userInfo_timeline_container, viewPager, TimelineContainerSwitcher.MAIN_FRAGMENT_TAG)
          .commitNow();
    } else {
      viewPager = (UserInfoPagerFragment) mainFragment;
      viewPager.setTabLayout(binding.userInfoTabs);
    }
    timelineContainerSwitcher = new TimelineContainerSwitcher(binding.userInfoTimelineContainer, viewPager, binding.ffab);
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
    Timber.tag(TAG).d("start: ");
    final Intent intent = createIntent(activity.getApplicationContext(), user.getId());
    final String transitionName = getUserIconTransitionName(user.getId());
    ViewCompat.setTransitionName(userIcon, transitionName);
    ActivityCompat.startActivity(activity, intent,
        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, userIcon, transitionName).toBundle());
  }

  static String getUserIconTransitionName(long id) {
    return "user_icon_" + id;
  }

  private long getUserId() {
    return getIntent().getLongExtra("user", -1L);
  }

  public static void bindUserScreenName(TextView textView, User user) {
    final Resources resources = textView.getContext().getResources();
    textView.setText(
        String.format(resources.getString(R.string.tweet_screenName), user.getScreenName()));
  }

  @Override
  public void onBackPressed() {
    if (toolbarTweetInputToggle != null && toolbarTweetInputToggle.isOpened()) {
      toolbarTweetInputToggle.cancelInput();
      onTweetInputClosed();
      tearDownTweetInputView();
      return;
    }
    if (timelineContainerSwitcher.clearSelectedCursorIfNeeded()) {
      return;
    }
    if (binding.ffab.getFabMode() == IndicatableFFAB.MODE_SHEET) {
      binding.ffab.transToFAB();
      return;
    }
    if (timelineContainerSwitcher.popBackStackTimelineContainer()) {
      return;
    }
    super.onBackPressed();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (toolbarTweetInputToggle != null && toolbarTweetInputToggle.onOptionMenuSelected(item)) {
      final int itemId = item.getItemId();
      if (itemId == R.id.action_sendTweet) {
        onTweetInputClosed();
      } else if (itemId == R.id.action_resumeTweet) {
        onTweetInputOpened();
      } else if (itemId == android.R.id.home) {
        onTweetInputClosed();
        tearDownTweetInputView();
      }
    }
    return super.onOptionsItemSelected(item);
  }

  private int titleVisibility;
  private ToolbarTweetInputToggle toolbarTweetInputToggle;

  private void showTwitterInputView(@TweetType int type, long statusId) {
    if (toolbarTweetInputToggle != null) {
      return;
    }
    toolbarTweetInputToggle = new ToolbarTweetInputToggle(binding.userInfoToolbar);
    getSupportFragmentManager().beginTransaction()
        .hide(userInfoAppbarFragment)
        .add(R.id.userInfo_appbar_container, toolbarTweetInputToggle.getFragment())
        .commitNow();
    toolbarTweetInputToggle.expandTweetInputView(type, statusId);
    onTweetInputOpened();
  }

  @Inject
  AppViewModelProviderFactory factory;

  public void onTweetInputOpened() {
    binding.userInfoAppbarContainer.setPadding(0, binding.userInfoToolbar.getHeight(), 0, 0);
    if (userInfoAppbarFragment.isVisible()) {
      getSupportFragmentManager().beginTransaction()
          .hide(userInfoAppbarFragment)
          .commitNow();
    }
    titleVisibility = binding.userInfoToolbarTitle.getVisibility();
    binding.userInfoToolbarTitle.setVisibility(View.GONE);
    binding.userInfoAppbarLayout.setExpanded(true);
    final TweetInputViewModel tweetInputViewModel = ViewModelProviders.of(this, factory).get(TweetInputViewModel.class);
    tweetUploadSubs = tweetInputViewModel.observeModel()
        .map(TweetInputModel::getState)
        .filter(state -> state == TweetInputModel.State.SENT)
        .subscribe(s -> tearDownTweetInputView());
  }

  public void onTweetInputClosed() {
    if (toolbarTweetInputToggle == null) {
      return;
    }
    binding.userInfoAppbarContainer.setPadding(0, 0, 0, 0);
    binding.userInfoAppbarLayout.setExpanded(titleVisibility != View.VISIBLE);
    binding.userInfoToolbarTitle.setVisibility(titleVisibility);
    getSupportFragmentManager().beginTransaction()
        .show(userInfoAppbarFragment)
        .commit();
  }

  private void tearDownTweetInputView() {
    getSupportFragmentManager().beginTransaction()
        .remove(toolbarTweetInputToggle.getFragment())
        .commit();
    toolbarTweetInputToggle = null;
  }

  private void setupActionMap() {
    binding.ffab.setOnIffabItemSelectedListener(item -> {
      final int itemId = item.getItemId();
      final long selectedTweetId = timelineContainerSwitcher.getSelectedTweetId();
      if (itemId == R.id.iffabMenu_main_reply) {
        showTwitterInputView(TYPE_REPLY, selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_quote) {
        showTwitterInputView(TYPE_QUOTE, selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_detail) {
        timelineContainerSwitcher.showStatusDetail(selectedTweetId);
      } else if (itemId == R.id.iffabMenu_main_conv) {
        timelineContainerSwitcher.showConversation(selectedTweetId);
      }
      fabViewModel.onMenuItemSelected(item);
    });
  }

  @Override
  public View getRootView() {
    return binding.userInfoTimelineContainer;
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    if (user.getId() == getUserId()) {
      return;
    }
    start(this, user, view);
  }

  @Override
  public void onSpanClicked(View v, SpanItem item) {
    if (item.getType() == SpanItem.TYPE_HASHTAG) {
      timelineContainerSwitcher.showSearchResult(item.getQuery());
    }
  }

  @Override
  public void onItemClicked(ContentType type, long id, String query) {
    if (type == ContentType.LISTS) {
      timelineContainerSwitcher.showListTimeline(id, query);
    }
  }

  @Inject
  DispatchingAndroidInjector<Fragment> androidInjector;

  @Override
  public AndroidInjector<Fragment> supportFragmentInjector() {
    return androidInjector;

  }
}
