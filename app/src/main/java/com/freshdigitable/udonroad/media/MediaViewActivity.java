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

package com.freshdigitable.udonroad.media;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.databinding.ActivityMediaViewBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.TwitterListItem;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import timber.log.Timber;
import twitter4j.MediaEntity;
import twitter4j.Status;

import static com.freshdigitable.udonroad.Utils.getBindingStatus;

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
  StatusRequestWorker statusRequestWorker;
  private MediaPagerAdapter mediaPagerAdapter;

  public static Intent create(@NonNull Context context, @NonNull TwitterListItem item) {
    return create(context, item, 0);
  }

  public static Intent create(@NonNull Context context, @NonNull TwitterListItem item, int startPage) {
    if (item.getMediaCount() < startPage + 1) {
      throw new IllegalArgumentException(
          "startPage number exceeded ExtendedMediaEntities length: " + startPage);
    }

    Intent intent = new Intent(context, MediaViewActivity.class);
    intent.putExtra(CREATE_STATUS, item.getId());
    intent.putExtra(CREATE_START, startPage);
    return intent;
  }

  public static void start(@NonNull Context context, @NonNull TwitterListItem item, int startPage) {
    final Intent intent = create(context, item, startPage);
    context.startActivity(intent);
  }

  public static void start(@NonNull Context context, long itemId, int startPage) {
    Intent intent = new Intent(context, MediaViewActivity.class);
    intent.putExtra(CREATE_STATUS, itemId);
    intent.putExtra(CREATE_START, startPage);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_media_view);
    InjectionUtil.getComponent(this).inject(this);

    ViewCompat.setElevation(binding.mediaToolbar,
        getResources().getDimensionPixelOffset(R.dimen.action_bar_elevation));
    setSupportActionBar(binding.mediaToolbar);
    showSystemUI();

    final ActionBar actionBar = getSupportActionBar();
    final IndicatableFFAB mediaIffab = binding.mediaIffab;
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(visibility -> {
      Timber.tag(TAG).d("onSystemUiVisibilityChange: %s", visibility);
      if (isSystemUIVisible(visibility)) {
        showOverlayUI(actionBar, mediaIffab);
      } else {
        hideOverlayUI(actionBar, mediaIffab);
      }
    });
  }

  private boolean isSystemUIVisible() {
    return isSystemUIVisible(getWindow().getDecorView().getSystemUiVisibility());
  }

  private static boolean isSystemUIVisible(int visibility) {
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
    if (isInMultiWindowModeCompat()) {
      showOverlayUI(getSupportActionBar(), binding.mediaIffab);
    }
  }

  private static void showOverlayUI(ActionBar actionBar, IndicatableFFAB iffab) {
    if (actionBar != null) {
      actionBar.show();
    }
    iffab.show();
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
    if (isInMultiWindowModeCompat()) {
      hideOverlayUI(getSupportActionBar(), binding.mediaIffab);
    }
  }

  private static void hideOverlayUI(ActionBar actionBar, IndicatableFFAB iffab) {
    if (actionBar != null) {
      actionBar.hide();
    }
    iffab.hide();
  }

  private void setTitleWithPosition(int position, int all) {
    final int currentItem = position + 1;
    setTitle(currentItem + " / " + all);
  }

  @Inject
  TypedCache<Status> statusCache;

  @Override
  protected void onStart() {
    super.onStart();
    statusCache.open();

    final Intent intent = getIntent();
    final long statusId = intent.getLongExtra(CREATE_STATUS, -1);
    final Status status = statusCache.find(statusId);
    if (status == null) {
      Toast.makeText(this, R.string.msg_media_failed_tweetNotFound, Toast.LENGTH_SHORT).show();
      return;
    }
    final MediaEntity[] bindingMediaEntities = getBindingStatus(status).getMediaEntities();
    final int startPage = intent.getIntExtra(CREATE_START, 0);
    mediaPagerAdapter = new MediaPagerAdapter(getSupportFragmentManager(), bindingMediaEntities);
    binding.mediaPager.setAdapter(mediaPagerAdapter);
    final int pages = bindingMediaEntities.length;
    binding.mediaPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
        setTitleWithPosition(position, pages);
      }
    });
    binding.mediaPager.setCurrentItem(startPage);
    setTitleWithPosition(startPage, pages);
    setupActionMap(statusId);
  }

  @Override
  protected void onStop() {
    super.onStop();
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
    binding.mediaPager.clearOnPageChangeListeners();
    binding.mediaPager.setAdapter(null);
    binding.mediaPager.removeAllViews();
    mediaPagerAdapter.clear();
    mediaPagerAdapter = null;
    binding.mediaIffab.clear();
    statusCache.close();
  }

  @Override
  public void onClick(View view) {
    Timber.tag(TAG).d("onClick: page");
    if (isSystemUIVisible()) {
      hideSystemUI();
    } else {
      showSystemUI();
    }
  }

  private static class MediaPagerAdapter extends FragmentPagerAdapter {
    private final List<MediaFragment> pages;

    MediaPagerAdapter(FragmentManager fm, MediaEntity[] mediaEntities) {
      super(fm);
      pages = new ArrayList<>(mediaEntities.length);
      for (MediaEntity me : mediaEntities) {
        pages.add(MediaFragment.create(me));
      }
    }

    @Override
    public Fragment getItem(int position) {
      return pages.get(position);
    }

    @Override
    public int getCount() {
      return pages.size();
    }

    void clear() {
      pages.clear();
      setPrimaryItem(null, -1, null); // XXX
    }
  }

  public static abstract class MediaFragment extends Fragment {
    private static MediaFragment create(MediaEntity mediaEntity) {
      final String type = mediaEntity.getType();
      if ("photo".equals(type)) {
        return PhotoMediaFragment.getInstance(mediaEntity);
      } else {
        // video and animated_gif are distributed as a mp4
        return SurfaceMediaView.getInstance(mediaEntity);
      }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
      if (container != null) {
        container.setBackgroundColor(Color.BLACK);
      }
      return super.onCreateView(inflater, container, savedInstanceState);
    }

    View.OnClickListener getOnClickListener() {
      final FragmentActivity activity = getActivity();
      if (activity instanceof View.OnClickListener) {
        return (View.OnClickListener) activity;
      }
      return null;
    }

    View.OnTouchListener getTouchListener() {
      return new View.OnTouchListener() {
        private static final double LOWER_THRESHOLD = (90 - 10) * Math.PI / 180;
        private static final double UPPER_THRESHOLD = (90 + 10) * Math.PI / 180;
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
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(new ContextWrapperImpl(newBase));
  }

  private static class ContextWrapperImpl extends ContextWrapper {
    public ContextWrapperImpl(Context base) {
      super(base);
    }

    @Override
    public Object getSystemService(String name) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
          && Context.AUDIO_SERVICE.equals(name)) {
        return getApplicationContext().getSystemService(name);
      }
      return super.getSystemService(name);
    }
  }

  private void setupActionMap(final long statusId) {
    binding.mediaIffab.setOnIffabItemSelectedListener(
        statusRequestWorker.getOnIffabItemSelectedListener(statusId));
  }

  private boolean isInMultiWindowModeCompat() {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
        && isInMultiWindowMode();
  }
}
