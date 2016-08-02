/*
 * Copyright (c) 2016. Akihito Matsuda (akihito104)
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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;

import javax.inject.Inject;

import twitter4j.User;

/**
 * Created by akihit on 2016/01/30.
 */
public class UserInfoActivity extends AppCompatActivity {
  private UserInfoPagerFragment viewPager;
  private ActivityUserInfoBinding binding;
  @Inject
  StatusCache statusCache;

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

    viewPager = (UserInfoPagerFragment) getSupportFragmentManager().findFragmentById(R.id.userInfo_pagerFragment);
    viewPager.setTabLayout(binding.userInfoTabs);
    viewPager.setFABHelper(new FlingableFABHelper(binding.fabIndicator, binding.ffab));
    viewPager.setUser(userId);
  }

  private void setUpUserInfoView(long userId) {
    final UserInfoFragment userInfoAppbarFragment = UserInfoFragment.create(userId);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.userInfo_appbar_container, userInfoAppbarFragment)
        .commit();
  }

  private void setUpAppbar() {
    binding.userInfoToolbar.setTitle("");

    final TextView toolbarTitle = binding.userInfoToolbarTitle;
    binding.userInfoAppbarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
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
    final User user = statusCache.getUser(userId);
    UserInfoActivity.bindUserScreenName(binding.userInfoToolbarTitle, user);

    setSupportActionBar(binding.userInfoToolbar);
  }

  @Override
  protected void onStop() {
    binding.userInfoToolbarTitle.setText("");
    binding.userInfoCollapsingToolbar.setTitleEnabled(false);
    binding.userInfoTabs.removeAllTabs();
    statusCache.close();
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.user_info, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    switch (itemId){
      case R.id.userInfo_heading:
        viewPager.scrollToTop();
        break;
      case R.id.userInfo_following:
        // todo following action
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  public static Intent createIntent(Context context, User user) {
    Intent intent = new Intent(context, UserInfoActivity.class);
    intent.putExtra("user", user.getId());
    return intent;
  }

  public static void start(Activity activity, User user, View userIcon) {
    final Intent intent = createIntent(activity.getApplicationContext(), user);
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
}
