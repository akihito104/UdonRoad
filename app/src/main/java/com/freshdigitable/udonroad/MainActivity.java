/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.ActivityMainBinding;
import com.squareup.picasso.Picasso;

import rx.Observable;
import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.Status;
import twitter4j.User;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding binding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment tlFragment;
//  private MainAppbarFragment appbarFragment;
  private TwitterApi twitterApi;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!TwitterApi.hasAccessToken(this)) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
      return;
    }
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    twitterApi = TwitterApi.setup(this);

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

    setupAppBar();
  }

  private void setupAppBar() {
//    appbarFragment = (MainAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.main_appbar_fragment);
//    appbarFragment = new MainAppbarFragment();
//    appbarFragment.setUserObservable(twitterApi.verifyCredentials());
//    getSupportFragmentManager().beginTransaction()
//        .replace(R.id.main_appbar_fragment, appbarFragment)
//        .commit();
//    attachToolbar(appbarFragment.getToolbar());
    binding.mainTweetInputView.setUserObservable(twitterApi.verifyCredentials());
    final InputMethodManager inputMethodManager
        = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    binding.mainTweetInputView.setOnInputFieldFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    });

  }

  private void setupUserTimeline() {
    tlFragment = (TimelineFragment) getSupportFragmentManager().findFragmentById(R.id.main_timeline);
    tlFragment.setUserIconClickedListener(new TimelineAdapter.OnUserIconClickedListener() {
      @Override
      public void onClicked(User user) {
        showUserInfo(user);
        tlFragment.showUserTimeline(user); // XXX: WTF
      }
    });
  }

  private void showUserInfo(User user) {
    binding.mainUserInfoView.setVisibility(View.VISIBLE);
    binding.mainCollapsingToolbar.setTitleEnabled(true);
    binding.mainCollapsingToolbar.setCollapsedTitleTextColor(Color.WHITE);
    binding.mainCollapsingToolbar.setExpandedTitleColor(Color.TRANSPARENT);
    binding.mainCollapsingToolbar.setTitle("@" + user.getScreenName());
    binding.mainUserInfoView.bindData(user);
  }

  private void dismissUserInfo() {
    binding.mainUserInfoView.setVisibility(View.GONE);
    binding.mainCollapsingToolbar.setTitleEnabled(false);
  }

  private void attachToolbar() {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        binding.navDrawerLayout, binding.mainToolbar, R.string.drawer_open, R.string.draver_close) {
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

    binding.mainToolbar.setTitleTextColor(Color.WHITE);
    setSupportActionBar(binding.mainToolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
  }

  private void setupNavigationDrawer() {
    twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<User>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.d(TAG, "twitter exception: " + e.toString());
          }

          @Override
          public void onNext(final User user) {
            ((TextView) binding.navDrawer.findViewById(R.id.nav_header_account)).setText(user.getScreenName());
            ImageView icon = (ImageView) binding.navDrawer.findViewById(R.id.nav_header_icon);
            Picasso.with(binding.navDrawer.getContext()).load(user.getProfileImageURLHttps()).fit().into(icon);
            icon.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                startActivity(UserAccountActivity.createIntent(MainActivity.this, user));
              }
            });
          }
        });
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
    setTitle("Home");
    attachToolbar();
    setupUserTimeline();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
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
//    if (appbarFragment.isStatusInputViewVisible()) {
    if (binding.mainTweetInputView.isVisible()) {
      cancelWritingSelected();
      return;
    }
    if (tlFragment.isTweetSelected()) {
      tlFragment.clearSelectedTweet();
      return;
    }
    if (binding.mainUserInfoView.getVisibility() == View.VISIBLE) {
      dismissUserInfo();
      tlFragment.showDefaultTimeline();
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
//      if (!appbarFragment.isStatusInputViewVisible()) {
      if (!binding.mainTweetInputView.isVisible()) {
        sendStatusSelected();
      } else {
        cancelWritingSelected();
      }
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  private void headingSelected() {
    tlFragment.scrollToTop();
  }

  private void sendStatusSelected() {
    sendStatusMenuItem.setIcon(R.drawable.ic_clear_white_24dp);
    tlFragment.setStopScroll(true);
//    appbarFragment.stretchStatusInputView(new TweetInputView.OnStatusSending() {
    binding.mainTweetInputView.appearing(new TweetInputView.OnStatusSending() {
      @Override
      public Observable<Status> sendStatus(String status) {
        return twitterApi.updateStatus(status);
      }

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
//    appbarFragment.collapseStatusInputView();
    binding.mainTweetInputView.disappearing();
  }

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }
}