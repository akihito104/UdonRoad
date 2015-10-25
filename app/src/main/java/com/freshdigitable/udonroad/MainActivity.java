package com.freshdigitable.udonroad;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
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

  @ViewById(R.id.timeline)
  protected RecyclerView timeline;

  private TimelineAdapter tlAdapter;
  private RecyclerView.LayoutManager tlLayoutManager;
  private final RecyclerView.ItemDecoration itemDecoration = new MyItemDecoration();

  private static class MyItemDecoration extends RecyclerView.ItemDecoration {
    MyItemDecoration() {
      paint.setColor(Color.GRAY);
    }
    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
      outRect.set(0, 0, 0, 2);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
      super.onDraw(c, parent, state);
      c.drawRect(0, 0, 0, 2, paint);
    }
  }

  @AfterViews
  void afterViews() {
    if (!AccessUtil.hasAccessToken(this)) {
      Intent intent = new Intent(this, OAuthActivity_.class);
      startActivity(intent);
      finish();
    }

    timeline.setHasFixedSize(true);
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
}

