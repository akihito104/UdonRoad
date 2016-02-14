package com.freshdigitable.udonroad;

import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
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
  private FragmentMainAppbarBinding binding;
  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_main_appbar, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    // android:titleTextColor is required up to API level 23
    binding.mainToolbar.setTitleTextColor(Color.WHITE);
    binding.mainTweetInputView.setUserObservable(userObservable);
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

  private InputMethodManager inputMethodManager;

  public void setInputMethodManager(final InputMethodManager inputMethodManager) {
    this.inputMethodManager = inputMethodManager;
  }

  public Toolbar getToolbar() {
    return binding.mainToolbar;
  }
}
