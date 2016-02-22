/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */
package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
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

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Paging;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.User;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getSimpleName();
  private ActivityMainBinding activityMainBinding;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private TimelineFragment tlFragment;
  private MainAppbarFragment appbarFragment;
  private TwitterApi twitterApi;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!TwitterApi.hasAccessToken(this)) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
      return;
    }
    activityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    twitterApi = TwitterApi.setup(this);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    activityMainBinding.navDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
          Log.d(TAG, "home is selected");
          activityMainBinding.navDrawerLayout.closeDrawer(activityMainBinding.navDrawer);
        } else if (itemId == R.id.menu_mention) {
          Log.d(TAG, "mention is selected");
          activityMainBinding.navDrawerLayout.closeDrawer(activityMainBinding.navDrawer);
        } else if (itemId == R.id.menu_fav) {
          Log.d(TAG, "fav is selected");
          activityMainBinding.navDrawerLayout.closeDrawer(activityMainBinding.navDrawer);
        }
        return false;
      }
    });

    setupUserTimeline();
    fetchTweet();
  }

  private void setupUserTimeline() {
    appbarFragment = (MainAppbarFragment) getSupportFragmentManager().findFragmentById(R.id.main_appbar_fragment);
    final InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    appbarFragment.setInputMethodManager(inputMethodManager);
    appbarFragment.setUserObservable(twitterApi.verifyCredentials());
    attachToolbar(appbarFragment.getToolbar());

    tlFragment = (TimelineFragment)getSupportFragmentManager().findFragmentById(R.id.main_timeline);
    tlFragment.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId - 1));
      }
    });
    tlFragment.setOnFlingForSelectedStatusListener(new TimelineFragment.OnFlingForSelectedStatusListener() {
      @Override
      public void onFling(FlingableFloatingActionButton.Direction direction, Status status) {
        if (FlingableFloatingActionButton.Direction.UP.equals(direction)) {
          fetchFavorite(status.getId());
        } else if (FlingableFloatingActionButton.Direction.RIGHT.equals(direction)) {
          fetchRetweet(status.getId());
        }
      }
    });
  }


  private void attachToolbar(Toolbar toolbar) {
    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        activityMainBinding.navDrawerLayout, toolbar, R.string.drawer_open, R.string.draver_close) {
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
    activityMainBinding.navDrawerLayout.setDrawerListener(actionBarDrawerToggle);

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    actionBarDrawerToggle.syncState();
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
            ((TextView) activityMainBinding.navDrawer.findViewById(R.id.nav_header_account)).setText(user.getScreenName());
            ImageView icon = (ImageView) activityMainBinding.navDrawer.findViewById(R.id.nav_header_icon);
            Picasso.with(activityMainBinding.navDrawer.getContext()).load(user.getProfileImageURLHttps()).fit().into(icon);
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
    super.onResume();
    twitterApi.connectUserStream(statusListener);
  }

  @Override
  protected void onPause() {
    twitterApi.disconnectStreamListener();
    super.onPause();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (activityMainBinding.navDrawerLayout.isDrawerOpen(activityMainBinding.navDrawer)) {
        activityMainBinding.navDrawerLayout.closeDrawer(activityMainBinding.navDrawer);
        return true;
      }
      if (appbarFragment.isStatusInputViewVisible()) {
        appbarFragment.collapseStatusInputView();
        tlFragment.setStopScroll(false);
        return true;
      }
      if (tlFragment.isTweetSelected()) {
        tlFragment.clearSelectedTweet();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
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
    tlFragment.scrollTo(0);
    tlFragment.clearSelectedTweet();
  }

  private void sendStatusSelected() {
    sendStatusMenuItem.setIcon(R.drawable.ic_clear_white_24dp);
    tlFragment.setStopScroll(true);
    appbarFragment.stretchStatusInputView(new TweetInputView.OnStatusSending() {
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
    appbarFragment.collapseStatusInputView();
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {
    @Override
    public void onStatus(final Status status) {
      Observable.just(status)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Status>() {
            @Override
            public void call(Status status) {
              tlFragment.addNewStatus(status);
            }
          });
    }

    @Override
    public void onDeletionNotice(final StatusDeletionNotice statusDeletionNotice) {
      Log.d(TAG, statusDeletionNotice.toString());
      Observable.just(statusDeletionNotice.getStatusId())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Long>() {
            @Override
            public void call(Long deletedStatusId) {
              tlFragment.deleteStatus(deletedStatusId);
            }
          });
    }

    @Override
    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
      Log.d(TAG, "onTrackLimitationNotice: " + numberOfLimitedStatuses);
    }

    @Override
    public void onScrubGeo(long userId, long upToStatusId) {
      Log.d(TAG, "onScrubGeo: " + userId + ", " + upToStatusId);
    }

    @Override
    public void onStallWarning(StallWarning warning) {
      Log.d(TAG, "onStallWarning: " + warning.toString());
    }

    @Override
    public void onException(Exception ex) {
      Log.d(TAG, "onException: " + ex.toString());
    }
  };

  private void fetchRetweet(final long tweetId) {
    twitterApi.retweetStatus(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Status>() {
          @Override
          public void onCompleted() {
            showToast("success to retweet");
          }

          @Override
          public void onError(Throwable e) {
            showToast("failed to retweet...");
          }

          @Override
          public void onNext(Status status) {
          }
        });
  }

  private void fetchFavorite(final long tweetId) {
    twitterApi.createFavorite(tweetId)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<Status>() {
          @Override
          public void onCompleted() {
            showToast("success to create fav.");
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "error: ", e);
            showToast("failed to create fav...");
          }

          @Override
          public void onNext(Status user) {
          }
        });
  }

  private void fetchTweet() {
    twitterApi.getHomeTimeline()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "home timeline is not downloaded.", e);
          }

          @Override
          public void onNext(List<Status> status) {
            tlFragment.addNewStatuses(status);
          }
        });
  }

  private void fetchTweet(Paging paging) {
    twitterApi.getHomeTimeline(paging)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Subscriber<List<Status>>() {
          @Override
          public void onCompleted() {
          }

          @Override
          public void onError(Throwable e) {
            Log.e(TAG, "home timeline is not downloaded.", e);
          }

          @Override
          public void onNext(List<Status> status) {
            tlFragment.addStatusesAtLast(status);
          }
        });
  }

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }
}