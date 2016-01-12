package com.freshdigitable.udonroad;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.support.v7.widget.Toolbar;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.User;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

@EActivity(R.layout.nav_drawer)
@OptionsMenu(R.menu.appbar_menu)
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();

  @ViewById(R.id.timeline)
  protected RecyclerView timeline;

  private TimelineAdapter tlAdapter;

  @ViewById(R.id.fab)
  protected FlingableFloatingActionButton ffab;

  @ViewById(R.id.toolbar)
  protected Toolbar toolbar;

  @ViewById(R.id.nav_drawer_layout)
  protected DrawerLayout drawerLayout;

  private ActionBarDrawerToggle actionBarDrawerToggle;
  private LinearLayoutManager tlLayoutManager;
  private TwitterApi twitterApi;

  @AfterViews
  protected void afterViews() {
    if (!TwitterApi.hasAccessToken(this)) {
      startActivity(new Intent(this, OAuthActivity_.class));
      finish();
    }
    twitterApi = TwitterApi.setup(this);

    timeline.setHasFixedSize(true);
    RecyclerView.ItemDecoration itemDecoration = new MyItemDecoration();
    timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(this);
    timeline.setLayoutManager(tlLayoutManager);

    tlAdapter = new TimelineAdapter();
    tlAdapter.setOnSelectedTweetChangeListener(selectedTweetChangeListener);
    timeline.setAdapter(tlAdapter);
    timeline.setItemAnimator(new TimelineAnimator());
    fetchTweet();

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    // android:titleTextColor is required up to API level 23
    toolbar.setTitleTextColor(Color.WHITE);

    actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawer_open, R.string.draver_close);
    actionBarDrawerToggle.setDrawerIndicatorEnabled(true);
    drawerLayout.setDrawerListener(actionBarDrawerToggle);

    setSupportActionBar(toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    actionBarDrawerToggle.syncState();

    setupNavigationDrawer();
    navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_home) {
          Log.d(TAG, "home is selected");
          drawerLayout.closeDrawer(navigationView);
        } else if (itemId == R.id.menu_mention) {
          Log.d(TAG, "mention is selected");
          drawerLayout.closeDrawer(navigationView);
        } else if (itemId == R.id.menu_fav) {
          Log.d(TAG, "fav is selected");
          drawerLayout.closeDrawer(navigationView);
        }
        return false;
      }
    });

    editTweet = (EditText) tweetInputView.findViewById(R.id.tw_intext);
    editTweet.setOnFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
      }
    });

    ffab.setVisibility(View.GONE);
    ffab.setOnFlingListener(new FlingableFloatingActionButton.OnFlingListener() {
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
  }

  private final TimelineAdapter.OnSelectedTweetChangeListener selectedTweetChangeListener
      = new TimelineAdapter.OnSelectedTweetChangeListener() {
    @Override
    public void onTweetSelected() {
      ffab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onTweetUnselected() {
      ffab.setVisibility(View.GONE);
    }
  };


  @ViewById(R.id.nav_drawer)
  protected NavigationView navigationView;

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
          public void onNext(User user) { /* TODO: save profile data to sqlite */
            ((TextView) navigationView.findViewById(R.id.nav_header_account)).setText(user.getScreenName());
            ImageView icon = (ImageView) navigationView.findViewById(R.id.nav_header_icon);
            Picasso.with(navigationView.getContext()).load(user.getProfileImageURLHttps()).fit().into(icon);

            ((TextView) tweetInputView.findViewById(R.id.tw_name)).setText(user.getName());
            ((TextView) tweetInputView.findViewById(R.id.tw_account)).setText(user.getScreenName());
            ImageView ticon = (ImageView) tweetInputView.findViewById(R.id.tw_icon);
            Picasso.with(tweetInputView.getContext()).load(user.getProfileImageURLHttps()).fit().into(ticon);
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
  public boolean onOptionsItemSelected(MenuItem item) {
    return actionBarDrawerToggle.onOptionsItemSelected(item)
        || super.onOptionsItemSelected(item);
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (drawerLayout.isDrawerOpen(navigationView)) {
        drawerLayout.closeDrawer(navigationView);
        return true;
      }
      if (tweetInputView.getVisibility() == View.VISIBLE) {
        tweetInputView.setVisibility(View.GONE);
        return true;
      }
      if (tlAdapter.isTweetSelected()) {
        tlAdapter.clearSelectedTweet();
        return true;
      }
    }
    return super.onKeyDown(keyCode, event);
  }

  @OptionsItem(R.id.action_heading)
  protected void headingSelected() {
    timeline.smoothScrollToPosition(0);
    tlAdapter.clearSelectedTweet();
  }

  @ViewById(R.id.tweet_input_view)
  protected View tweetInputView;
  private EditText editTweet;

  @SystemService
  InputMethodManager inputMethodManager;

  @OptionsItem(R.id.action_write)
  protected void tweetSelected() {
    tweetInputView.setVisibility(View.VISIBLE);
    editTweet.requestFocus();
    tweetInputView.findViewById(R.id.tw_send_intweet).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        final String sendingText = editTweet.getText().toString();
        if (sendingText.length() <= 0) {
          return;
        }
        final ImageButton sendButton = (ImageButton) tweetInputView.findViewById(R.id.tw_send_intweet);
        sendButton.setClickable(false);
        twitterApi.updateStatus(sendingText)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Status>() {
              @Override
              public void onCompleted() {
                sendButton.setClickable(true);
              }

              @Override
              public void onError(Throwable e) {
                showToast("send tweet: failure...");
                Log.e(TAG, "update status: " + e);
              }

              @Override
              public void onNext(Status status) {
                editTweet.getText().clear();
                editTweet.clearFocus();
                tweetInputView.setVisibility(View.GONE);
              }
            });
      }
    });
  }

  private boolean canScrollToAdd() {
    int firstVisibleItem = tlLayoutManager.findFirstVisibleItemPosition();
    return firstVisibleItem == 0
        && tlAdapter.getSelectedTweetId() <= 0
        && tweetInputView.getVisibility() == View.GONE;
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
                timeline.smoothScrollToPosition(0);
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

  private void showToast(String text) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
  }

  @Click(R.id.fab)
  protected void onFabClicked() {}

  private static class MyItemDecoration extends RecyclerView.ItemDecoration {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int dividerHeight;

    MyItemDecoration() {
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