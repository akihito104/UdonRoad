package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import com.freshdigitable.udonroad.databinding.FragmentMainAppbarBinding;

import rx.Observable;
import twitter4j.User;

/**
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
    binding.mainTweetInputView.setUserObservable(userObservable);
    final InputMethodManager inputMethodManager
        = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    binding.mainTweetInputView.setOnInputFieldFocusChangeListener(new View.OnFocusChangeListener() {
      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        if (hasFocus) {
          inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_FORCED);
        } else {
          inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
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

  public void stretchStatusInputView(TweetInputView.OnStatusSending statusSending) {
    binding.mainTweetInputView.appearing(statusSending);
  }

  public void collapseStatusInputView() {
    binding.mainTweetInputView.disappearing();
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
}
