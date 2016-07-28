package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTweetAppbarBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.squareup.picasso.Picasso;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.User;

/**
 * TweetAppbarFragment provides Appbar with TweetInputView.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetAppbarFragment extends Fragment {
  private static final String TAG = TweetAppbarFragment.class.getSimpleName();
  private FragmentTweetAppbarBinding binding;
  @Inject
  TwitterApi twitterApi;
  @Inject
  StatusCache statusCache;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: ");
    View view = inflater.inflate(R.layout.fragment_tweet_appbar, container, false);
    binding = DataBindingUtil.bind(view);
    return view;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
    binding.mainToolbar.setTitle("Home");
  }

  @Override
  public void onStart() {
    super.onStart();
    statusCache.open(getContext());

    final AppCompatActivity activity = (AppCompatActivity) getActivity();
    activity.setSupportActionBar(binding.mainToolbar);
    final ActionBar supportActionBar = activity.getSupportActionBar();
    if (supportActionBar != null) {
      supportActionBar.setDisplayHomeAsUpEnabled(true);
      supportActionBar.setHomeButtonEnabled(true);
    }
  }

  @Override
  public void onStop() {
    statusCache.close();
    super.onStop();
  }

  private FloatingActionButton tweetSendFab;

  public void setTweetSendFab(FloatingActionButton fab) {
    this.tweetSendFab = fab;
  }

  private TextWatcher textWatcher = new TextWatcher() {
    @Override
    public void afterTextChanged(Editable editable) {
      if (editable.length() < 1) {
        tweetSendFab.setEnabled(false);
      } else {
        tweetSendFab.setEnabled(true);
      }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }
  };

  public void stretchStatusInputView(final OnStatusSending statusSending) {
    stretchStatusInputView(statusSending, -1);
  }

  public void stretchStatusInputView(final OnStatusSending statusSending, long inReplyToStatusId) {
    if (inReplyToStatusId < 1) {
      stretchStatusInputView(statusSending, null);
      return;
    }
    final Status status = statusCache.getStatus(inReplyToStatusId);
    stretchStatusInputView(statusSending, status);
  }

  public void stretchStatusInputView(final OnStatusSending statusSending, @Nullable final Status inReplyTo) {
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inReplyTo != null) {
      inputText.addText("@" + inReplyTo.getUser().getScreenName() + " "); // XXX
    }

    twitterApi.verifyCredentials()
        .observeOn(AndroidSchedulers.mainThread())
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

    inputText.addTextWatcher(textWatcher);
    if (inputText.getText().length() < 1) {
      tweetSendFab.setEnabled(false);
    }

    tweetSendFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        String sendingText = inputText.getText().toString();
        v.setClickable(false);
        final Observable<Status> statusObservable;
        if (inReplyTo != null) {
          final StatusUpdate statusUpdate = new StatusUpdate(sendingText)
              .inReplyToStatusId(inReplyTo.getId());
          statusObservable = twitterApi.updateStatus(statusUpdate);
        } else {
          statusObservable = twitterApi.updateStatus(sendingText);
        }
        statusObservable
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
    inputText.appearing();
  }

  public void collapseStatusInputView() {
    binding.mainTweetInputView.removeTextWatcher(textWatcher);
    binding.mainTweetInputView.disappearing();
    tweetSendFab.setOnClickListener(null);
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  public Toolbar getToolbar() {
    return binding.mainToolbar;
  }

  interface OnStatusSending {
    void onSuccess(Status status);

    void onFailure(Throwable e);
  }
}