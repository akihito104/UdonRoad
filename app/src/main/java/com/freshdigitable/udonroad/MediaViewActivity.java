/*
 * Copyright (c) 2016. Matsuda, Akihit (akihito104)
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

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.freshdigitable.udonroad.UserAction.Resource;
import com.freshdigitable.udonroad.databinding.ActivityMediaViewBinding;
import com.freshdigitable.udonroad.datastore.MediaCache;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.OnFlingListener.Direction;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.TimelineSubscriber;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;

/**
 * MediaViewActivity shows picture or video in Status.
 *
 * Created by akihit on 2016/07/12.
 */
public class MediaViewActivity extends AppCompatActivity implements View.OnClickListener {
  @SuppressWarnings("unused")
  private static final String TAG = MediaViewActivity.class.getSimpleName();
  private static final String CREATE_STATUS = "status";
  private static final String CREATE_START = "start";

  private ActivityMediaViewBinding binding;
  @Inject
  TwitterApi twitterApi;
  private Handler handler;
  @Inject
  TypedCache<Status> statusCache;
  private TimelineSubscriber<TypedCache<Status>> userActionSubscriber;
  private Map<Direction, UserAction> actionMap = new HashMap<>();

  public static Intent create(@NonNull Context context, @NonNull Status status) {
    return create(context, status, 0);
  }

  public static Intent create(@NonNull Context context, @NonNull Status status, int startPage) {
    final Status bindingStatus = StatusViewImageHelper.getBindingStatus(status);
    if (bindingStatus.getExtendedMediaEntities().length < startPage + 1) {
      throw new IllegalArgumentException(
          "startPage number exceeded ExtendedMediaEntities length: " + startPage);
    }

    Intent intent = new Intent(context, MediaViewActivity.class);
    intent.putExtra(CREATE_STATUS, status.getId());
    intent.putExtra(CREATE_START, startPage);
    return intent;
  }

  public static void start(@NonNull Context context, @NonNull Status status, int startPage) {
    final Intent intent = create(context, status, startPage);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showSystemUI();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_media_view);
    InjectionUtil.getComponent(this).inject(this);

    ViewCompat.setElevation(binding.mediaToolbar,
        getResources().getDimensionPixelOffset(R.dimen.action_bar_elevation));
    setSupportActionBar(binding.mediaToolbar);

    handler = new Handler();

    final ActionBar actionBar = getSupportActionBar();
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
        new View.OnSystemUiVisibilityChangeListener() {
          @Override
          public void onSystemUiVisibilityChange(int visibility) {
            Log.d(TAG, "onSystemUiVisibilityChange: " + visibility);
            if (isSystemUIVisible(visibility)) {
              if (actionBar != null) {
                setTitle();
                actionBar.show();
                binding.mediaIffab.show();
              }
              binding.mediaIffab.show();
            } else {
              if (actionBar != null) {
                actionBar.hide();
              }
              binding.mediaIffab.hide();
            }
          }
        });
  }

  private boolean isSystemUIVisible() {
    return isSystemUIVisible(getWindow().getDecorView().getSystemUiVisibility());
  }

  private boolean isSystemUIVisible(int visibility) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return (View.SYSTEM_UI_FLAG_FULLSCREEN & visibility) == 0;
    } else {
      return (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION & visibility) == 0;
    }
  }

  private void showSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      getWindow().getDecorView().setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
      );
    }
  }

  private void hideSystemUI() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      getWindow().getDecorView().setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
              | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
              | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
              | View.SYSTEM_UI_FLAG_IMMERSIVE
      );
    } else {
      getWindow().getDecorView().setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
      );
    }
  }

  private void setTitle() {
    final int all = binding.mediaPager.getAdapter().getCount();
    final int currentItem = binding.mediaPager.getCurrentItem() + 1;
    setTitle(currentItem + " / " + all);
  }

  private final ViewPager.OnPageChangeListener pageChangeListener
      = new ViewPager.SimpleOnPageChangeListener() {
    @Override
    public void onPageSelected(int position) {
      super.onPageSelected(position);
      setTitle();
    }
  };

  @Override
  protected void onStart() {
    super.onStart();
    statusCache.open();
    userActionSubscriber = new TimelineSubscriber<>(twitterApi, statusCache,
        new FeedbackSubscriber.ToastFeedback(getApplicationContext(), Gravity.CENTER, 0, 0));

    final Intent intent = getIntent();
    final long statusId = intent.getLongExtra(CREATE_STATUS, -1);
    final Status status = statusCache.find(statusId);
    if (status == null) {
      Toast.makeText(getApplicationContext(), "status is not found", Toast.LENGTH_SHORT).show();
      return;
    }
    final Status bindingStatus = StatusViewImageHelper.getBindingStatus(status);
    final int startPage = intent.getIntExtra(CREATE_START, 0);
    binding.mediaPager.setAdapter(
        new MediaPagerAdapter(getSupportFragmentManager(), bindingStatus.getExtendedMediaEntities())
    );
    binding.mediaPager.addOnPageChangeListener(pageChangeListener);
    binding.mediaPager.setCurrentItem(startPage);
    setTitle();
    setupActionMap(statusId);
    UserAction.setupFlingableFAB(binding.mediaIffab, actionMap, getApplicationContext());
  }

  @Override
  protected void onStop() {
    handler.removeCallbacksAndMessages(null);
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
    binding.mediaPager.removeOnPageChangeListener(pageChangeListener);
    binding.mediaPager.setAdapter(null);
    binding.mediaIffab.setOnFlingListener(null);
    actionMap.clear();
    statusCache.close();
    super.onStop();
  }

  private final Runnable hideSystemUITask = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "handler.postDelayed: ");
      hideSystemUI();
    }
  };

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    Log.d(TAG, "onWindowFocusChanged: " + Boolean.toString(hasFocus));
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      handler.postDelayed(hideSystemUITask, 1000);
    }
  }

  @Override
  public void onClick(View view) {
    Log.d(TAG, "onClick: page");
    if (isSystemUIVisible()) {
      hideSystemUI();
    } else {
      showSystemUI();
    }
  }

  private static class MediaPagerAdapter extends FragmentPagerAdapter {
    private ExtendedMediaEntity[] mediaEntities;

    MediaPagerAdapter(FragmentManager fm, ExtendedMediaEntity[] mediaEntities) {
      super(fm);
      this.mediaEntities = mediaEntities;
    }

    @Override
    public Fragment getItem(int position) {
      return MediaFragment.create(mediaEntities[position]);
    }

    @Override
    public int getCount() {
      return mediaEntities.length;
    }
  }

  public static abstract class MediaFragment extends Fragment {
    private static MediaFragment create(ExtendedMediaEntity mediaEntity) {
      final MediaFragment fragment;
      final String type = mediaEntity.getType();
      if ("photo".equals(type)) {
        fragment = new PhotoMediaFragment();
      } else {
        // video and animated_gif are distributed as a mp4
        fragment = new VideoMediaFragment();
      }
      final Bundle args = new Bundle();
      args.putLong("media_id", mediaEntity.getId());
      fragment.setArguments(args);
      return fragment;
    }

    protected ExtendedMediaEntity mediaEntity;
    protected View.OnClickListener pageClickListener;
    @Inject
    MediaCache mediaCache;

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      InjectionUtil.getComponent(this).inject(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
      if (container != null) {
        container.setBackgroundColor(Color.BLACK);
      }
      final FragmentActivity activity = getActivity();
      if (activity instanceof View.OnClickListener) {
        pageClickListener = (View.OnClickListener) activity;
      }
      return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
      super.onStart();
      mediaCache.open();
      final long mediaId = getArguments().getLong("media_id");
      mediaEntity = mediaCache.getMediaEntity(mediaId);
    }

    @Override
    public void onStop() {
      super.onStop();
      mediaCache.close();
    }

    protected final View.OnTouchListener touchListener = new View.OnTouchListener() {
      final double LOWER_THRESHOLD = (90 - 10) * Math.PI / 180;
      final double UPPER_THRESHOLD = (90 + 10) * Math.PI / 180;
      private MotionEvent old;

      @Override
      public boolean onTouch(View view, MotionEvent now) {
        final int action = now.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
          old = MotionEvent.obtain(now);
          return false;
        }
        if (action == MotionEvent.ACTION_UP) {
          try {
            final float deltaX = now.getX() - old.getX();
            final float deltaY = now.getY() - old.getY();
            final double powDist = deltaX * deltaX + deltaY * deltaY;
            if (powDist < 100) {  // maybe click
//              Log.d(TAG, "onTouch: click: " + powDist);
              return false;
            }
            final double rad = Math.atan2(deltaY, deltaX);
            final double absRad = Math.abs(rad);
            if (absRad > LOWER_THRESHOLD || absRad < UPPER_THRESHOLD) { // maybe swipe to longitude
              return true;
            }
          } finally {
            old.recycle();
          }
        }
        return false;
      }
    };
  }

  private void setupActionMap(final long statusId) {
    actionMap.put(Direction.UP, new UserAction(Resource.FAV, new Runnable() {
      @Override
      public void run() {
        userActionSubscriber.createFavorite(statusId);
      }
    }));
    actionMap.put(Direction.RIGHT, new UserAction(Resource.RETWEET, new Runnable() {
      @Override
      public void run() {
        userActionSubscriber.retweetStatus(statusId);
      }
    }));
    actionMap.put(Direction.UP_RIGHT, new UserAction());
  }
}
