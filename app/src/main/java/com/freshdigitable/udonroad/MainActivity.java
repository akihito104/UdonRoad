/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.TimelineAdapter.OnUserIconClickedListener;
import com.freshdigitable.udonroad.TweetAppbarFragment.OnStatusSending;
import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.freshdigitable.udonroad.ffab.FlingableFABHelper;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.User;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment tlFragment;
  private TweetAppbarFragment appbarFragment;

  @Inject
  TwitterApi twitterApi;
  private FlingableFABHelper flingableFABHelper;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    InjectionUtil.getComponent(this).inject(this);
    if (!twitterApi.loadAccessToken()) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
      return;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    binding.navDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
          Log.d(TAG, "home is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.menu_mention) {
          Log.d(TAG, "mention is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        } else if (itemId == R.id.menu_fav) {
          Log.d(TAG, "fav is selected");
          binding.navDrawerLayout.closeDrawer(binding.navDrawer);
        }
        return false;
      }
    });

    binding.ffab.hide();
    flingableFABHelper = new FlingableFABHelper(binding.fabIndicator, binding.ffab);
    setupAppBar();
    setupHomeTimeline();
  }

  private void setupAppBar() {
    appbarFragment = (TweetAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.tweet_appbar);
    appbarFragment.setTweetSendFab(binding.mainSendTweet);
  }

  private void setupHomeTimeline() {
    tlFragment = new HomeTimelineFragment();
    tlFragment.setUserIconClickedListener(new OnUserIconClickedListener() {
      @Override
      public void onClicked(View view, User user) {
        showUserInfo(view, user);
      }
    });
    tlFragment.setFABHelper(flingableFABHelper);
    tlFragment.setupOnFlingListener();
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.main_timeline_container, tlFragment)
        .commit();
  }

  private void showUserInfo(View view, User user) {
    if (appbarFragment.isStatusInputViewVisible()) {
      return;
    }
    binding.ffab.hide();
    UserInfoActivity.start(this, user, view);
  }

  private void attachToolbar(Toolbar toolbar) {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, toolbar, R.string.drawer_open, R.string.draver_close) {
      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        setupNavigationDrawer();
        tlFragment.setStopScroll(true);
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        tlFragment.setStopScroll(false);
      }
    };
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    binding.navDrawerLayout.addDrawerListener(actionBarDrawerToggle);
    actionBarDrawerToggle.syncState();
  }

  private void setupNavigationDrawer() {
    twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            new Action1<User>() {
              @Override
              public void call(final User user) {
                final TextView account
                    = (TextView) binding.navDrawer.findViewById(R.id.nav_header_account);
                account.setText(user.getScreenName());
                final ImageView icon
                    = (ImageView) binding.navDrawer.findViewById(R.id.nav_header_icon);
                Picasso.with(binding.navDrawer.getContext())
                    .load(user.getProfileImageURLHttps()).fit()
                    .into(icon);
              }
            },
            new Action1<Throwable>() {
              @Override
              public void call(Throwable throwable) {
                Log.d(TAG, "twitter exception: " + throwable.toString());
              }
            }
        );
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    actionBarDrawerToggle.syncState();
  }

  @Override
  protected void onResume() {
    Log.d(TAG, "onResume: ");
    super.onResume();
    attachToolbar(appbarFragment.getToolbar());
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    binding.navDrawer.setNavigationItemSelectedListener(null);
    appbarFragment.setTweetSendFab(null);
    tlFragment.setUserIconClickedListener(null);
    tlFragment.setFABHelper(null);
    super.onDestroy();
  }

  @Override
  public void onBackPressed() {
    if (tlFragment.hideStatusDetail()) {
      return;
    }
    if (binding.navDrawerLayout.isDrawerOpen(binding.navDrawer)) {
      binding.navDrawerLayout.closeDrawer(binding.navDrawer);
      return;
    }
    if (appbarFragment.isStatusInputViewVisible()) {
      cancelWritingSelected();
      return;
    }
    if (tlFragment.isTweetSelected()) {
      tlFragment.clearSelectedTweet();
      return;
    }
    super.onBackPressed();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    actionBarDrawerToggle.onConfigurationChanged(newConfig);
  }

  private MenuItem sendStatusMenuItem;

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.appbar_menu, menu);
    sendStatusMenuItem = menu.findItem(R.id.action_write);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int itemId = item.getItemId();
    if (itemId == R.id.action_heading){
      headingSelected();
    } else if (itemId == R.id.action_write) {
      if (!appbarFragment.isStatusInputViewVisible()) {
        sendStatusSelected();
      } else {
        cancelWritingSelected();
      }
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void headingSelected() {
    if (tlFragment.isVisible()) {
      tlFragment.scrollToTop();
    }
  }

  private void sendStatusSelected() {
    if (binding.ffab.getVisibility() == View.VISIBLE) {
      binding.ffab.hide();
    }
    sendStatusMenuItem.setIcon(R.drawable.ic_clear_white_24dp);
    tlFragment.setStopScroll(true);
    appbarFragment.stretchStatusInputView(new OnStatusSending() {
      @Override
      public void onSuccess(Status status) {
        cancelWritingSelected();
      }

      @Override
      public void onFailure(Throwable e) {
        showToast("send tweet: failure...");
        Log.e(TAG, "update status: " + e);
      }
    });
  }

  private void cancelWritingSelected() {
    sendStatusMenuItem.setIcon(R.drawable.ic_create_white_24dp);
    tlFragment.setStopScroll(false);
    appbarFragment.collapseStatusInputView();
    if (tlFragment.isTweetSelected()) {
      binding.ffab.show();
    }
  }

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }
}