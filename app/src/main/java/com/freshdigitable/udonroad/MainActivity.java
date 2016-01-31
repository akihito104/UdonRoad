package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.databinding.DataBindingUtil;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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
import com.freshdigitable.udonroad.databinding.FragmentTimelineBinding;
import com.freshdigitable.udonroad.databinding.TweetInputViewBinding;
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
  private static final String TAG = MainActivity.class.getName();
  private FragmentTimelineBinding activityMainBinding;
  private ActivityMainBinding navDrawerBinding;

  private TimelineAdapter tlAdapter;
  private ActionBarDrawerToggle actionBarDrawerToggle;
  private LinearLayoutManager tlLayoutManager;
  private TwitterApi twitterApi;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    navDrawerBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    activityMainBinding = navDrawerBinding.mainLayout;

    if (!TwitterApi.hasAccessToken(this)) {
      startActivity(new Intent(this, OAuthActivity.class));
      finish();
    }
    twitterApi = TwitterApi.setup(this);
    inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    activityMainBinding.timeline.setHasFixedSize(true);
    RecyclerView.ItemDecoration itemDecoration = new MyItemDecoration();
    activityMainBinding.timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(this);
    activityMainBinding.timeline.setLayoutManager(tlLayoutManager);
    activityMainBinding.timeline.setItemAnimator(new TimelineAnimator());

    tlAdapter = new TimelineAdapter();
    tlAdapter.setOnSelectedTweetChangeListener(selectedTweetChangeListener);
    activityMainBinding.timeline.setAdapter(tlAdapter);
    tlAdapter.setLastItemBoundListener(new TimelineAdapter.LastItemBoundListener() {
      @Override
      public void onLastItemBound(long statusId) {
        fetchTweet(new Paging(1, 20, 1, statusId-1));
      }
    });
    fetchTweet();

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    // android:titleTextColor is required up to API level 23
    activityMainBinding.toolbar.setTitleTextColor(Color.WHITE);

    actionBarDrawerToggle = new ActionBarDrawerToggle(this,
        navDrawerBinding.navDrawerLayout, activityMainBinding.toolbar, R.string.drawer_open, R.string.draver_close);
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    navDrawerBinding.navDrawerLayout.setDrawerListener(actionBarDrawerToggle);

    setSupportActionBar(activityMainBinding.toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    actionBarDrawerToggle.syncState();

    setupNavigationDrawer();
    navDrawerBinding.navDrawer.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
          Log.d(TAG, "home is selected");
          navDrawerBinding.navDrawerLayout.closeDrawer(navDrawerBinding.navDrawer);
        } else if (itemId == R.id.menu_mention) {
          Log.d(TAG, "mention is selected");
          navDrawerBinding.navDrawerLayout.closeDrawer(navDrawerBinding.navDrawer);
        } else if (itemId == R.id.menu_fav) {
          Log.d(TAG, "fav is selected");
          navDrawerBinding.navDrawerLayout.closeDrawer(navDrawerBinding.navDrawer);
        }
        return false;
      }
    });

    activityMainBinding.tweetInputView.setOnInputFieldFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    });

    activityMainBinding.fab.setVisibility(View.GONE);
    activityMainBinding.fab.setOnFlingListener(new FlingableFloatingActionButton.OnFlingListener() {
      @Override
      public void onFling(FlingableFloatingActionButton.Direction direction) {
        Log.d(TAG, "fling direction: " + direction.toString());
        if (!tlAdapter.isTweetSelected()) {
          return;
        }
        final long tweetId = tlAdapter.getSelectedTweetId();
        if (FlingableFloatingActionButton.Direction.UP.equals(direction)) {
          fetchFavorite(tweetId);
        } else if (FlingableFloatingActionButton.Direction.LEFT.equals(direction)) {
          fetchRetweet(tweetId);
        }
      }
    });
    activityMainBinding.fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
      }
    });
  }

  private final TimelineAdapter.OnSelectedTweetChangeListener selectedTweetChangeListener
      = new TimelineAdapter.OnSelectedTweetChangeListener() {
    @Override
    public void onTweetSelected() {
      activityMainBinding.fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTweetUnselected() {
      activityMainBinding.fab.setVisibility(View.GONE);
    }
  };

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
          public void onNext(final User user) { /* TODO: save profile data to sqlite */
            ((TextView) navDrawerBinding.navDrawer.findViewById(R.id.nav_header_account)).setText(user.getScreenName());
            ImageView icon = (ImageView) navDrawerBinding.navDrawer.findViewById(R.id.nav_header_icon);
            Picasso.with(navDrawerBinding.navDrawer.getContext()).load(user.getProfileImageURLHttps()).fit().into(icon);
            icon.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                startActivity(UserAccountActivity.createIntent(MainActivity.this, user));
              }
            });

            activityMainBinding.tweetInputView.setUserInfo(user);
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
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    actionBarDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.appbar_menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_heading){
      headingSelected();
    } else if (item.getItemId() == R.id.action_write) {
      tweetSelected();
    }
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (navDrawerBinding.navDrawerLayout.isDrawerOpen(navDrawerBinding.navDrawer)) {
        navDrawerBinding.navDrawerLayout.closeDrawer(navDrawerBinding.navDrawer);
        return true;
      }
      if (activityMainBinding.tweetInputView.isVisible()) {
        activityMainBinding.tweetInputView.disappearing();
        return true;
      }
      if (tlAdapter.isTweetSelected()) {
        tlAdapter.clearSelectedTweet();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  private void headingSelected() {
    activityMainBinding.timeline.smoothScrollToPosition(0);
    tlAdapter.clearSelectedTweet();
  }

  private InputMethodManager inputMethodManager;

  private void tweetSelected() {
    activityMainBinding.tweetInputView.appearing(new TweetInputView.OnStatusSending() {
      @Override
      public void sendingStatus(final TweetInputViewBinding binding) {
        final String sendingText = binding.twIntext.getText().toString();
        if (sendingText.isEmpty()) {
          return;
        }
        binding.twSendIntweet.setClickable(false);
        twitterApi.updateStatus(sendingText)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Status>() {
              @Override
              public void onCompleted() {
                binding.twSendIntweet.setClickable(true);
              }

              @Override
              public void onError(Throwable e) {
                showToast("send tweet: failure...");
                Log.e(TAG, "update status: " + e);
              }

              @Override
              public void onNext(Status status) {
                binding.twIntext.getText().clear();
                binding.twIntext.clearFocus();
                activityMainBinding.tweetInputView.setVisibility(View.GONE);
              }
            });

      }
    });
  }

  private boolean canScrollToAdd() {
    int firstVisibleItem = tlLayoutManager.findFirstVisibleItemPosition();
    return firstVisibleItem == 0
        && tlAdapter.getSelectedTweetId() <= 0
        && activityMainBinding.tweetInputView.getVisibility() == View.GONE;
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {
    @Override
    public void onStatus(final Status status) {
      Observable.just(status)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new Action1<Status>() {
            @Override
            public void call(Status status) {
              if (canScrollToAdd()) {
                tlAdapter.addNewStatus(status);
                activityMainBinding.timeline.smoothScrollToPosition(0);
              }else {
                tlAdapter.addNewStatus(status);
              }
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
              tlAdapter.deleteStatus(deletedStatusId);
              tlAdapter.notifyDataSetChanged();
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
            tlAdapter.addNewStatuses(status);
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
            tlAdapter.addNewStatusesAtLast(status);
          }
        });
  }

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  private static class MyItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint;
    private final int dividerHeight;

    MyItemDecoration() {
      paint = new Paint(Paint.ANTI_ALIAS_FLAG);
      paint.setColor(Color.GRAY);
      this.dividerHeight = 1;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
      int position = ((RecyclerView.LayoutParams) view.getLayoutParams()).getViewLayoutPosition();
      int top = position == 0 ? 0 : dividerHeight;
      outRect.set(0, top, 0, 0);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
      super.onDraw(c, parent, state);
      final float left = parent.getPaddingLeft();
      final float right = parent.getWidth() - parent.getPaddingRight();
      final int childCount = parent.getChildCount();
      final RecyclerView.LayoutManager manager = parent.getLayoutManager();

      for (int i = 0; i < childCount; i++) {
        final View child = parent.getChildAt(i);
        final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
        if (params.getViewLayoutPosition() == 0) {
          continue;
        }
        final float top = manager.getDecoratedTop(child) - params.topMargin
            + ViewCompat.getTranslationY(child);
        final float bottom = top + dividerHeight;
        c.drawRect(left, top, right, bottom, paint);
      }
    }
  }
}