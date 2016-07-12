/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.freshdigitable.udonroad.realmdata.ReferredStatusRealm;
import com.freshdigitable.udonroad.realmdata.StatusRealm;
import com.squareup.picasso.Picasso;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import twitter4j.ExtendedMediaEntity;
import twitter4j.Status;

/**
 * Created by akihit on 2016/07/12.
 */
public class MediaViewActivity extends AppCompatActivity {
  private static final String CREATE_STATUS = "status";
  private static final String CREATE_START = "start";
  private final ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT);
  private Realm realm;
  private ViewPager viewPager;

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

    viewPager = new ViewPager(this);
    viewPager.setId(R.id.user_pager);
    setContentView(viewPager, layoutParams);

    showSystemUI();
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
              | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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
    viewPager.setAdapter(new MediaPagerAdapter(getSupportFragmentManager(),
        status.getExtendedMediaEntities()));

    final Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        hideSystemUI();
      }
    }, 350);
  }

  @Override
  protected void onStop() {
    realm.close();
    super.onStop();
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
