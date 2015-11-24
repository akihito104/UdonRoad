package com.freshdigitable.udonroad;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.support.v7.widget.Toolbar;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.OptionsMenuItem;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import twitter4j.ResponseList;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.UserStreamAdapter;
import twitter4j.UserStreamListener;

@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.appbar_menu)
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();

  @ViewById(R.id.timeline)
  protected RecyclerView timeline;

  private TimelineAdapter tlAdapter;
  private RecyclerView.LayoutManager tlLayoutManager;
  private RecyclerView.ItemDecoration itemDecoration;
  private Twitter twitter;
  private TwitterStream twitterStream;

  @ViewById(R.id.fab)
  protected FlingableFloatingActionButton ffab;

  @ViewById(R.id.toolbar)
  protected Toolbar toolbar;

  @AfterViews
  protected void afterViews() {
    if (!AccessUtil.hasAccessToken(this)) {
      OAuthActivity_.intent(this).start();
      finish();
    }

    timeline.setHasFixedSize(true);
    itemDecoration = new MyItemDecoration();
    timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(this);
    timeline.setLayoutManager(tlLayoutManager);

    ffab.setOnFlingListener(new FlingableFloatingActionButton.OnFlingListener() {
      @Override
      public void onFling(FlingableFloatingActionButton.Direction direction) {
        final long tweetId = tlAdapter.getSelectedTweetId();
        if (tweetId < 0) {
          return;
        }
        if (FlingableFloatingActionButton.Direction.UP.equals(direction)) {
          fetchFavorite(tweetId);
        } else if (FlingableFloatingActionButton.Direction.LEFT.equals(direction)) {
          fetchRetweet(tweetId);
        }
      }
    });

    tlAdapter = new TimelineAdapter();
    timeline.setAdapter(tlAdapter);
    twitter = AccessUtil.getTwitterInstance(this);
    timeline.setItemAnimator(new TimelineAnimator());
    twitterStream = AccessUtil.getTwitterStreamInstance(this);
    fetchTweet();

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setSupportActionBar(toolbar);
  }

  @Override
  protected void onResume() {
    super.onResume();
    twitterStream.addListener(statusListener);
    twitterStream.user();
  }

  @Override
  protected void onPause() {
    twitterStream.clearListeners();
    twitterStream.shutdown();
    super.onPause();
  }

  @OptionsItem(R.id.action_heading)
  protected void headingSelected() {
    timeline.smoothScrollToPosition(0);
  }

  @ViewById(R.id.tl_inputview)
  View inputView;

  @OptionsItem(R.id.action_write)
  protected void tweetSelected() {
   TweetActivity_.intent(this).start();
//    if (inputView.getVisibility() == View.GONE) {
//      inputView.setVisibility(View.VISIBLE);
//    } else {
//      inputView.setVisibility(View.GONE);
//    }
  }

  private final UserStreamListener statusListener = new UserStreamAdapter() {
    @Override
    public void onStatus(Status status) {
      notifyUpdateTimeline(status);
    }

    @Override
    public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
      Log.d(TAG, statusDeletionNotice.toString());
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

  @Background
  protected void fetchRetweet(long tweetId) {
    try {
      twitter.retweetStatus(tweetId);
    } catch (TwitterException e) {
      Log.e(TAG, "error: ", e);
    }
  }

  @Background
  protected void fetchFavorite(long tweetId) {
    try {
      twitter.createFavorite(tweetId);
    } catch (TwitterException e) {
      Log.e(TAG, "error: ", e);
    }
  }

  @UiThread
  protected void notifyUpdateTimeline(Status status) {
    tlAdapter.addNewStatus(status);
  }

  @Background
  protected void fetchTweet() {
    try {
      ResponseList<Status> statuses = twitter.getHomeTimeline();
      updateTimeline(statuses);
    } catch (TwitterException e) {
      Log.e(TAG, "home timeline is not downloaded.", e);
    }
  }

  @UiThread
  protected void updateTimeline(List<Status> statuses) {
    tlAdapter.addNewStatuses(statuses);
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