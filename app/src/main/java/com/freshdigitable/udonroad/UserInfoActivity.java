package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.ActivityUserInfoBinding;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;

import twitter4j.User;

/**
 * Created by akihit on 2016/01/30.
 */
public class UserInfoActivity extends AppCompatActivity {
  private UserInfoPagerFragment viewPager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    ActivityUserInfoBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_user_info);
    binding.ffab.hide();

    long userId = parseIntent();
    final UserInfoAppbarFragment appbar = (UserInfoAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.userInfo_appbar);
    appbar.setUser(userId);

    viewPager = (UserInfoPagerFragment) getSupportFragmentManager().findFragmentById(R.id.userInfo_pagerFragment);
    viewPager.setTabLayout(appbar.getTabLayout());
    viewPager.setFABHelper(new FlingableFABHelper(binding.fabIndicator, binding.ffab));
    viewPager.setUser(userId);
  }

  @Override
  protected void onStop() {
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

  private long parseIntent() {
    return getIntent().getLongExtra("user", -1L);
  }

  public static void bindUserScreenName(TextView textView, User user) {
    final Resources resources = textView.getContext().getResources();
    textView.setText(
        String.format(resources.getString(R.string.tweet_name), user.getScreenName()));
  }
}
