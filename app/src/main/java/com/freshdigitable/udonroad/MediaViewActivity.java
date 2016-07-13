/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import com.freshdigitable.udonroad.databinding.ActivityMediaViewBinding;
import com.freshdigitable.udonroad.fab.FlingableFAB;
import com.freshdigitable.udonroad.fab.FlingableFABHelper;
import com.freshdigitable.udonroad.fab.OnFlingAdapter;
import com.freshdigitable.udonroad.fab.OnFlingListener.Direction;
import com.freshdigitable.udonroad.realmdata.ReferredStatusRealm;
import com.freshdigitable.udonroad.realmdata.StatusRealm;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;

/**
 * Created by akihit on 2016/07/12.
 */
public class MediaViewActivity extends AppCompatActivity {
  @SuppressWarnings("unused")
  private static final String TAG = MediaViewActivity.class.getSimpleName();
  private static final String CREATE_STATUS = "status";
  private static final String CREATE_START = "start";
  private static final int DURATION_SHOW_SYSTEM_UI = 5000;

  private Realm realm;
  private ActivityMediaViewBinding binding;
  @Inject
  TwitterApi twitterApi;
  private FlingableFABHelper ffabHelper;
  private Handler handler;
  private final Runnable hideSystemUITask = new Runnable() {
    @Override
    public void run() {
      Log.d(TAG, "handler.postDelayed: ");
      hideSystemUI();
    }
  };

  public static Intent create(@NonNull Context context, @NonNull Status status) {
    return create(context, status, 0);
  }

  public static Intent create(@NonNull Context context, @NonNull Status status, int startPage) {
    if (status.getExtendedMediaEntities().length < startPage + 1) {
      throw new IllegalArgumentException(
          "startPage number exceeded ExtendedMediaEntities length: " + startPage);
    }

    Intent intent = new Intent(context, MediaViewActivity.class);
    intent.putExtra(CREATE_STATUS, status.getId());
    intent.putExtra(CREATE_START, startPage);
    return intent;
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showSystemUI();
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_media_view);
    ((MainApplication) getApplication()).getTwitterApiComponent().inject(this);

    ViewCompat.setElevation(binding.mediaToolbar,
        getResources().getDimensionPixelOffset(R.dimen.action_bar_elevation));
    setSupportActionBar(binding.mediaToolbar);

    ffabHelper = new FlingableFABHelper(binding.mediaIndicator, binding.mediaFfab);
    handler = new Handler();

    final FlingableFAB mediaFfab = binding.mediaFfab;
    final ActionBar actionBar = getSupportActionBar();
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
        new View.OnSystemUiVisibilityChangeListener() {
      @Override
      public void onSystemUiVisibilityChange(int visibility) {
        Log.d(TAG, "onSystemUiVisibilityChange: " + visibility);
        if ((View.SYSTEM_UI_FLAG_FULLSCREEN & visibility) == 0) {
          showSystemUI();
          if (actionBar != null) {
            setTitle();
            actionBar.show();
          }
          mediaFfab.show();
          handler.postDelayed(hideSystemUITask, DURATION_SHOW_SYSTEM_UI);
        } else {
          if (actionBar != null) {
            actionBar.hide();
          }
          mediaFfab.hide();
        }
      }
    });
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

  private Status findStatus(long id) {
    final StatusRealm status = realm.where(StatusRealm.class)
        .equalTo("id", id)
        .findFirst();
    if (status != null) {
      if (status.getRetweetedStatusId() > 0) {
        final ReferredStatusRealm rtStatus = realm.where(ReferredStatusRealm.class)
            .equalTo("id", status.getRetweetedStatusId())
            .findFirst();
        status.setRetweetedStatus(rtStatus);
      }
      if (status.getQuotedStatusId() > 0) {
        final ReferredStatusRealm qtStatus = realm.where(ReferredStatusRealm.class)
            .equalTo("id", status.getQuotedStatusId())
            .findFirst();
        status.setQuotedStatus(qtStatus);
      }
      return status;
    } else {
      return realm.where(ReferredStatusRealm.class)
          .equalTo("id", id)
          .findFirst();
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
    realm = Realm.getInstance(
        new RealmConfiguration.Builder(getApplicationContext())
            .name("home").build());

    final Intent intent = getIntent();
    final long statusId = intent.getLongExtra(CREATE_STATUS, -1);
    final Status status = findStatus(statusId);
    final int startPage = intent.getIntExtra(CREATE_START, 0);
    binding.mediaPager.setAdapter(new MediaPagerAdapter(getSupportFragmentManager(),
        status.getExtendedMediaEntities()));
    binding.mediaPager.addOnPageChangeListener(pageChangeListener);
    binding.mediaPager.setCurrentItem(startPage);
    setTitle();

    ffabHelper.addEnableDirection(Direction.UP);
    ffabHelper.addEnableDirection(Direction.UP_RIGHT);
    binding.mediaFfab.setOnFlingListener(new OnFlingAdapter() {
      @Override
      public void onStart() {
        handler.removeCallbacksAndMessages(null);
      }

      @Override
      public void onFling(Direction direction) {
        switch (direction) {
          case UP:
            twitterApi.createFavorite(status.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
            break;
          case RIGHT:
            twitterApi.retweetStatus(status.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
            break;
          case UP_RIGHT:
            twitterApi.createFavorite(status.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
            twitterApi.retweetStatus(status.getId())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe();
            break;
        }
        handler.postDelayed(hideSystemUITask, DURATION_SHOW_SYSTEM_UI);
      }
    });
  }

  @Override
  protected void onStop() {
    handler.removeCallbacksAndMessages(null);
    getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
    binding.mediaPager.removeOnPageChangeListener(pageChangeListener);
    binding.mediaPager.setAdapter(null);
    binding.mediaFfab.setOnFlingListener(null);
    realm.close();
    super.onStop();
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    Log.d(TAG, "onWindowFocusChanged: " + Boolean.toString(hasFocus));
    super.onWindowFocusChanged(hasFocus);
    if (hasFocus) {
      handler.postDelayed(hideSystemUITask, 1000);
    }
  }

  private static class MediaPagerAdapter extends FragmentPagerAdapter {
    private ExtendedMediaEntity[] mediaEntities;

    public MediaPagerAdapter(FragmentManager fm, ExtendedMediaEntity[] mediaEntities) {
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

  public static class MediaFragment extends Fragment {
    private static Fragment create(ExtendedMediaEntity mediaEntity) {
      final MediaFragment fragment = new MediaFragment();
      fragment.mediaEntity = mediaEntity;
      return fragment;
    }

    private ExtendedMediaEntity mediaEntity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
      final ImageView imageView = new ImageView(getContext());
      imageView.setBackgroundColor(Color.BLACK);
      return imageView;
    }

    @Override
    public void onStart() {
      super.onStart();
      Picasso.with(getContext())
          .load(mediaEntity.getMediaURLHttps())
          .into((ImageView) getView());
    }

    @Override
    public void onStop() {
      Picasso.with(getContext()).cancelRequest((ImageView) getView());
      super.onStop();
    }
  }
}
