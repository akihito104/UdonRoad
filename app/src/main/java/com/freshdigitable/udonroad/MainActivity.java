package com.freshdigitable.udonroad;

import android.content.Intent;
import android.content.res.Resources;
import android.gesture.Gesture;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.List;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity {
  private static final String TAG = MainActivity.class.getName();
  private static final float SWIPE_MIN_DISTANCE = 120;
  private static final float SWIPE_THRESH_VER = 200;

  @ViewById(R.id.timeline)
  protected RecyclerView timeline;

  @ViewById(R.id.fab)
  protected FloatingActionButton fab;

  private TimelineAdapter tlAdapter;
  private RecyclerView.LayoutManager tlLayoutManager;
  private RecyclerView.ItemDecoration itemDecoration;

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

  @AfterViews
  void afterViews() {
    if (!AccessUtil.hasAccessToken(this)) {
      Intent intent = new Intent(this, OAuthActivity_.class);
      startActivity(intent);
      finish();
    }

    final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESH_VER) {
          Log.d(TAG, "fling to left.");
          return false;
        } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESH_VER) {
          Log.d(TAG, "fling to right.");
          return false;
        }
        if (e1.getY() - e2.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESH_VER) {
          Log.d(TAG, "fling to up");
          return false;
        } else if (e2.getY() - e1.getY() > SWIPE_MIN_DISTANCE && Math.abs(velocityY) > SWIPE_THRESH_VER) {
          Log.d(TAG, "fling to down");
          return false;
        }
        return false;
      }
    });
    fab.setOnTouchListener(new View.OnTouchListener() {
      @Override
      public boolean onTouch(View view, MotionEvent motionEvent) {
        gestureDetector.onTouchEvent(motionEvent);
        return true;
      }
    });

    timeline.setHasFixedSize(true);
    itemDecoration = new MyItemDecoration();
    timeline.addItemDecoration(itemDecoration);
    tlLayoutManager = new LinearLayoutManager(this);
    timeline.setLayoutManager(tlLayoutManager);

    fetchTweet();
  }

  @Background
  protected void fetchTweet() {
    Twitter twitter = AccessUtil.getTwitterInstance(this);
    try {
      ResponseList<Status> statuses = twitter.getHomeTimeline();
      updateTimeline(statuses);
    } catch (TwitterException e) {
      Log.e(TAG, "home timeline is not downloaded.", e);
    }
  }

  @UiThread
  protected void updateTimeline(List<Status> statuses) {
    tlAdapter = new TimelineAdapter(statuses);
    timeline.setAdapter(tlAdapter);
  }

  @Click(R.id.fab)
  protected void fabClicked() {
    Intent intent = new Intent(this, TweetActivity_.class);
    startActivity(intent);
  }
}