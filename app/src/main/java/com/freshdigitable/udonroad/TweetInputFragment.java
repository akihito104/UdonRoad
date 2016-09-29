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
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.User;
import twitter4j.UserMentionEntity;

/**
 * TweetInputFragment provides TweetInputView and logic to send tweet.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetInputFragment extends Fragment {
  private static final String TAG = TweetInputFragment.class.getSimpleName();
  private FragmentTweetInputBinding binding;
  @Inject
  TwitterApi twitterApi;
  @Inject
  TypedCache<Status> statusCache;
  @Inject
  ConfigSubscriber configSubscriber;

  public static TweetInputFragment create(@TweetType int type) {
    return create(type, -1);
  }

  public static TweetInputFragment create(@TweetType int type, long statusId) {
    final Bundle args = new Bundle();
    args.putInt("tweet_type", type);
    args.putLong("status_id", statusId);
    final TweetInputFragment tweetInputFragment = new TweetInputFragment();
    tweetInputFragment.setArguments(args);
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
    binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tweet_input, container, false);
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
    statusCache.open();
    configSubscriber.open();
    final Bundle arguments = getArguments();
    final @TweetType int tweetType = arguments.getInt("tweet_type");
    final long statusId = arguments.getLong("status_id", -1);
    stretchTweetInputView(tweetType, statusId);
  }

  @Override
  public void onStop() {
    super.onStop();
    configSubscriber.close();
    statusCache.close();
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

  public void stretchTweetInputView(@TweetType int type, long statusId) {
    if (type == TYPE_DEFAULT) {
      stretchTweetInputView();
    } else if (type == TYPE_REPLY) {
      stretchTweetInputViewWithInReplyTo(statusId);
    } else if (type == TYPE_QUOTE) {
      stretchTweetInputViewWithQuoteStatus(statusId);
    }
  }

  private void stretchTweetInputView() {
    setUpTweetInputView();
    setUpTweetSendFab();
    binding.mainTweetInputView.appearing();
  }

  private void stretchTweetInputViewWithInReplyTo(long inReplyToStatusId) {
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    stretchTweetInputViewWithInReplyTo(inReplyTo);
  }

  private void stretchTweetInputViewWithInReplyTo(Status inReplyTo) {
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inReplyTo != null) {
      inputText.addText(ReplyEntity.create(inReplyTo).createReplyString());
      inputText.setInReplyTo();
    }
    stretchTweetInputView();
  }

  private void stretchTweetInputViewWithQuoteStatus(long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
    stretchTweetInputView();
  }

  private void setUpTweetInputView() {
    final TweetInputView inputText = binding.mainTweetInputView;
    configSubscriber.getAuthenticatedUser()
        .subscribe(new Action1<User>() {
          @Override
          public void call(User authenticatedUser) {
            inputText.setUserInfo(authenticatedUser);
            Picasso.with(inputText.getContext())
                .load(authenticatedUser.getMiniProfileImageURLHttps())
                .fit()
                .into(inputText.getIcon());
          }
        });
    inputText.addTextWatcher(textWatcher);
    inputText.setShortUrlLength(
        configSubscriber.getConfigStore().getTwitterAPIConfig().getShortURLLengthHttps());
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
    tweetSendFab.setOnClickListener(createSendClickListener());
  }

  private View.OnClickListener createSendClickListener() {
    final FragmentActivity activity = getActivity();
    if (activity instanceof TweetSendable) {
      return new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
          v.setClickable(false);
          ((TweetSendable) activity).observeUpdateStatus(
              observeUpdateStatus()
                  .doOnCompleted(new Action0() {
                    @Override
                    public void call() {
                      v.setClickable(true);
                    }
                  })
          );
        }
      };
    } else {
      return new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          view.setClickable(false);
          observeUpdateStatus()
              .doOnCompleted(new Action0() {
                @Override
                public void call() {
                  view.setClickable(true);
                }
              })
              .subscribe();
        }
      };
    }
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
          final Status status = statusCache.find(q);
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

  private Observable<Status> observeUpdateStatus() {
    final TweetInputView inputText = binding.mainTweetInputView;
    return createSendObservable()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnNext(new Action1<Status>() {
          @Override
          public void call(Status status) {
            inputText.getText().clear();
            inputText.clearFocus();
            inputText.disappearing();
          }
        });
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

  interface TweetSendable {
    void setupInput(@TweetType int type, long statusId);

    void observeUpdateStatus(Observable<Status> updateStatusObservable);
  }

  public static final int TYPE_DEFAULT = 0;
  public static final int TYPE_REPLY = 1;
  public static final int TYPE_QUOTE = 2;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {TYPE_DEFAULT, TYPE_REPLY, TYPE_QUOTE})
  public @interface TweetType {
  }

  private static class ReplyEntity {
    long inReplyToStatusId;
    List<String> screenNames;

    static ReplyEntity create(Status status) { // XXX
      final ReplyEntity res = new ReplyEntity();
      res.inReplyToStatusId = status.getId();
      res.screenNames = new ArrayList<>();
      final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
      for (UserMentionEntity u : userMentionEntities) {
        res.addScreenName(u.getScreenName());
      }

      if (status.isRetweet()) {
        final Status retweetedStatus = status.getRetweetedStatus();
        res.addScreenName(retweetedStatus.getUser().getScreenName());
      }

      final User user = status.getUser();
      res.addScreenName(user.getScreenName());
      return res;
    }

    private void addScreenName(String screenName) {
      if (!screenNames.contains(screenName)) {
        screenNames.add(screenName);
      }
    }

    String createReplyString() {
      String s = "";
      for (String sn : screenNames) {
        s += "@" + sn + " ";
      }
      return s;
    }
  }
}