package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

import com.freshdigitable.udonroad.databinding.FragmentMainAppbarBinding;
import com.squareup.picasso.Picasso;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.User;

/**
 * MainAppbarFragment integrates Appbar parts.
 *
 * Created by akihit on 2016/02/06.
 */
public class MainAppbarFragment extends Fragment {
  private static final String TAG = MainAppbarFragment.class.getSimpleName();
  private FragmentMainAppbarBinding binding;
  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: ");
    View view = inflater.inflate(R.layout.fragment_main_appbar, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
    // android:titleTextColor is required up to API level 23
    binding.mainToolbar.setTitleTextColor(Color.WHITE);
    binding.mainToolbar.setTitle("Home");

    final TextView toolbarTitle = binding.mainToolbarTitle;
    binding.mainAppbarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
      // it is visible (alpha=1.0) at initial state.
      private boolean isTitleVisible = true;

      @Override
      public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
        final int totalScrollRange = appBarLayout.getTotalScrollRange();
        final float percent = (float) Math.abs(verticalOffset) / (float) totalScrollRange;
        if (percent > 0.9) {
          if (!isTitleVisible) {
            startAnimation(toolbarTitle, View.VISIBLE);
            isTitleVisible = true;
          }
        } else {
          if (isTitleVisible) {
            startAnimation(toolbarTitle, View.INVISIBLE);
            isTitleVisible = false;
          }
        }
      }

      private void startAnimation(View v, int visibility) {
        AlphaAnimation animation = (visibility == View.VISIBLE)
            ? new AlphaAnimation(0f, 1f)
            : new AlphaAnimation(1f, 0f);
        animation.setDuration(200);
        animation.setFillAfter(true);
        v.startAnimation(animation);
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();

    final AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = activity.getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
  }

  private FloatingActionButton tweetSendFab;

  public void setTweetSendFab(FloatingActionButton fab) {
    this.tweetSendFab = fab;
  }

  public void stretchStatusInputView(final OnStatusSending statusSending) {
    final TweetInputView inputText = binding.mainTweetInputView;
    userObservable.observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<User>() {
          @Override
          public void call(User user) {
            inputText.setUserInfo(user);
            Picasso.with(inputText.getContext()).load(
                user.getProfileImageURLHttps()).fit().into(inputText.getIcon());
          }
        }, new Action1<Throwable>() {
          @Override
          public void call(Throwable throwable) {
            Log.d(TAG, throwable.getMessage(), throwable);
          }
        });
    inputText.appearing();
    tweetSendFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        String sendingText = inputText.getText().toString();
        if (sendingText.isEmpty()) {
          return;
        }
        v.setClickable(false);
        statusSending.sendStatus(sendingText)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Subscriber<Status>() {
              @Override
              public void onNext(Status status) {
                inputText.getText().clear();
                inputText.clearFocus();
                statusSending.onSuccess(status);
                inputText.disappearing();
              }

              @Override
              public void onError(Throwable e) {
                statusSending.onFailure(e);
              }

              @Override
              public void onCompleted() {
                v.setClickable(true);
              }
            });
      }
    });
  }

  public void collapseStatusInputView() {
    binding.mainTweetInputView.disappearing();
    tweetSendFab.setOnClickListener(null);
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  private Observable<User> userObservable;

  public void setUserObservable(Observable<User> user) {
    this.userObservable = user;
  }

  public Toolbar getToolbar() {
    return binding.mainToolbar;
  }

  public boolean isUserInfoVisible() {
    return binding.mainUserInfoView.getVisibility() == View.VISIBLE;
  }

  public void showUserInfo(User user) {
    binding.mainUserInfoView.setVisibility(View.VISIBLE);
    binding.mainToolbar.setTitle("");
    binding.mainToolbarTitle.setText("@" + user.getScreenName());
    binding.mainUserInfoView.bindData(user);
  }

  public void dismissUserInfo() {
    binding.mainUserInfoView.setVisibility(View.GONE);
    binding.mainToolbarTitle.setText("");
    binding.mainToolbar.setTitle("Home");
    binding.mainCollapsingToolbar.setTitleEnabled(false);
    binding.mainTabs.setVisibility(View.GONE);
    binding.mainTabs.removeAllTabs();
  }

  public TabLayout getTabLayout() {
    return binding.mainTabs;
  }

  interface OnStatusSending {
    Observable<Status> sendStatus(String text);

    void onSuccess(Status status);

    void onFailure(Throwable e);
  }
}