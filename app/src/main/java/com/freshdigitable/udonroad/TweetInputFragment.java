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
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.ConfigStore;
import com.freshdigitable.udonroad.datastore.StatusCache;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.User;

/**
 * TweetInputFragment provides Appbar with TweetInputView.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetInputFragment extends Fragment {
  private static final String TAG = TweetInputFragment.class.getSimpleName();
  private FragmentTweetInputBinding binding;
  @Inject
  TwitterApi twitterApi;
  @Inject
  StatusCache statusCache;
  @Inject
  ConfigStore configStore;

  public static TweetInputFragment create(@TweetType int type, OnStatusSending statusSending) {
    return create(type, -1, statusSending);
  }

  public static TweetInputFragment create(@TweetType int type, long statusId, OnStatusSending statusSending) {
    final Bundle args = new Bundle();
    args.putInt("tweet_type", type);
    args.putLong("status_id", statusId);
    final TweetInputFragment tweetInputFragment = new TweetInputFragment();
    tweetInputFragment.setArguments(args);
    tweetInputFragment.setOnStatusSending(statusSending);
    return tweetInputFragment;
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    Log.d(TAG, "onCreateView: ");
    binding = FragmentTweetInputBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    Log.d(TAG, "onActivityCreated: ");
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public void onStart() {
    super.onStart();
    statusCache.open(getContext());
    configStore.open(getContext());
    final Bundle arguments = getArguments();
    final @TweetType int tweetType = arguments.getInt("tweet_type");
    final long statusId = arguments.getLong("status_id", -1);
    stretchTweetInputView(tweetType, statusId, statusSending);
  }

  @Override
  public void onStop() {
    configStore.close();
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
      tweetSendFab.setEnabled(editable.length() > 1);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }
  };

  private long inReplyToStatusId = -1;
  private List<Long> quoteStatusIds = new ArrayList<>(4);
  private OnStatusSending statusSending;

  public void setOnStatusSending(OnStatusSending statusSending) {
    this.statusSending = statusSending;
  }

  public void stretchTweetInputView(@TweetType int type, long statusId, OnStatusSending statusSending) {
    if (type == TYPE_DEFAULT) {
      stretchTweetInputView(statusSending);
    } else if (type == TYPE_REPLY) {
      stretchTweetInputViewWithInReplyTo(statusSending, statusId);
    } else if (type == TYPE_QUOTE) {
      stretchTweetInputViewWithQuoteStatus(statusSending, statusId);
    }
  }

  private void stretchTweetInputView(final OnStatusSending statusSending) {
    setUpTweetInputView();
    setUpTweetSendFab();
    binding.mainTweetInputView.appearing();
  }

  private void stretchTweetInputViewWithInReplyTo(final OnStatusSending statusSending, long inReplyToStatusId) {
    final Status inReplyTo = statusCache.findStatus(inReplyToStatusId);
    stretchTweetInputViewWithInReplyTo(statusSending, inReplyTo);
  }

  private void stretchTweetInputViewWithInReplyTo(final OnStatusSending statusSending, Status inReplyTo) {
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inReplyTo != null) {
      inputText.addText("@" + inReplyTo.getUser().getScreenName() + " "); // XXX
      inputText.setInReplyTo();
    }
    stretchTweetInputView(statusSending);
  }

  private void stretchTweetInputViewWithQuoteStatus(final OnStatusSending statusSending, long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
    stretchTweetInputView(statusSending);
  }

  private void setUpTweetInputView() {
    final TweetInputView inputText = binding.mainTweetInputView;
    final User authenticatedUser = configStore.getAuthenticatedUser();
    inputText.setUserInfo(authenticatedUser);
    Picasso.with(inputText.getContext())
        .load(authenticatedUser.getMiniProfileImageURLHttps())
        .fit()
        .into(inputText.getIcon());
    inputText.addTextWatcher(textWatcher);
    inputText.setShortUrlLength(
        configStore.getTwitterAPIConfig().getShortURLLengthHttps());
  }

  public void tearDownTweetInputView() {
    binding.mainTweetInputView.removeTextWatcher(textWatcher);
    binding.mainTweetInputView.reset();
  }

  private void setUpTweetSendFab() {
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inputText.getText().length() < 1) {
      tweetSendFab.setEnabled(false);
    }

    tweetSendFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(final View v) {
        v.setClickable(false);
        createSendObservable()
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

  private void tearDownSendTweetFab() {
    tweetSendFab.setOnClickListener(null);
  }

  private Observable<Status> createSendObservable() {
    final String sendingText = binding.mainTweetInputView.getText().toString();
    if (isNeedStatusUpdate()) {
      String s = sendingText;
      if (quoteStatusIds.size() > 0) {
        for (long q : quoteStatusIds) {
          final Status status = statusCache.findStatus(q);
          if (status == null) {
            continue;
          }
          s +=" https://twitter.com/" + status.getUser().getScreenName()
              + "/status/" + q;
        }
      }
      final StatusUpdate statusUpdate = new StatusUpdate(s);
      if (inReplyToStatusId > 0) {
        statusUpdate.setInReplyToStatusId(inReplyToStatusId);
      }
      return twitterApi.updateStatus(statusUpdate);
    } else {
      return twitterApi.updateStatus(sendingText);
    }
  }

  private boolean isNeedStatusUpdate() {
    return inReplyToStatusId > 0
        || quoteStatusIds.size() > 0;
  }

  public void addQuoteStatus(long quoteStatusId) {
    quoteStatusIds.add(quoteStatusId);
  }

  public void collapseStatusInputView() {
    tearDownTweetInputView();
    tearDownSendTweetFab();
    inReplyToStatusId = -1;
    quoteStatusIds.clear();
    binding.mainTweetInputView.disappearing();
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  interface OnStatusSending {
    void onSuccess(Status status);

    void onFailure(Throwable e);
  }

  public static final int TYPE_DEFAULT = 0;
  public static final int TYPE_REPLY = 1;
  public static final int TYPE_QUOTE = 2;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {TYPE_DEFAULT, TYPE_REPLY, TYPE_QUOTE})
  public @interface TweetType {
  }
}