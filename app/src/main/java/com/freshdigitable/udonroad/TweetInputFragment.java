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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.ConfigRequestWorker;
import com.freshdigitable.udonroad.subscriber.RequestWorkerBase;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscription;
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
  private static final String LOADINGTAG_TWEET_INPUT_ICON = "TweetInputIcon";
  private FragmentTweetInputBinding binding;
  @Inject
  StatusRequestWorker<TypedCache<Status>> statusRequestWorker;
  @Inject
  ConfigRequestWorker configRequestWorker;
  @Inject
  AppSettingStore appSettings;
  private Subscription subscription;

  public static TweetInputFragment create() {
    return create(TYPE_NONE);
  }

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

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  private MenuItem sendStatusMenuItem;
  private MenuItem cancelMenuItem;

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    Log.d(TAG, "onCreateOptionsMenu: ");
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.tweet_input, menu);
    sendStatusMenuItem = menu.findItem(R.id.action_write);
    cancelMenuItem = menu.findItem(R.id.action_cancel);
    setupMenuVisibility();
  }

  private void setupMenuVisibility() {
    if (sendStatusMenuItem != null) {
      sendStatusMenuItem.setVisible(!binding.mainTweetInputView.isVisible());
    }
    if (cancelMenuItem != null) {
      cancelMenuItem.setVisible(binding.mainTweetInputView.isVisible());
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(TAG, "onOptionsItemSelected: ");
    final int itemId = item.getItemId();
    if (itemId == R.id.action_write) {
      stretchTweetInputView();
    } else if (itemId == R.id.action_cancel) {
      collapseStatusInputView();
    }
    return false;
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
    statusRequestWorker.open();
    configRequestWorker.open();
    appSettings.open();
    final Bundle arguments = getArguments();
    final @TweetType int tweetType = arguments.getInt("tweet_type");
    final long statusId = arguments.getLong("status_id", -1);
    stretchTweetInputView(tweetType, statusId);
  }

  @Override
  public void onStop() {
    super.onStop();
    appSettings.close();
    configRequestWorker.close();
    statusRequestWorker.close();
  }

  private FloatingActionButton tweetSendFab;

  public void setTweetSendFab(FloatingActionButton fab) {
    this.tweetSendFab = fab;
  }

  private TextWatcher textWatcher = new TextWatcher() {
    @Override
    public void afterTextChanged(Editable editable) {
      tweetSendFab.setEnabled(editable.length() >= 1);
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }

    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
    }
  };

  private List<Long> quoteStatusIds = new ArrayList<>(4);

  public void stretchTweetInputView(@TweetType int type, long statusId) {
    if (type == TYPE_NONE) {
      return;
    }
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
    setupMenuVisibility();
  }

  private ReplyEntity replyEntity;

  private void stretchTweetInputViewWithInReplyTo(long inReplyToStatusId) {
    final Status inReplyTo = statusRequestWorker.getCache().find(inReplyToStatusId);
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inReplyTo == null) {
      stretchTweetInputView();
      return;
    }
    replyEntity = ReplyEntity.create(inReplyTo);
    inputText.addText(replyEntity.createReplyString());
    inputText.setInReplyTo();
    stretchTweetInputView();
  }

  private void stretchTweetInputViewWithQuoteStatus(long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
    stretchTweetInputView();
  }

  private void setUpTweetInputView() {
    final TweetInputView inputText = binding.mainTweetInputView;
    subscription = configRequestWorker.getAuthenticatedUser()
        .subscribe(new Action1<User>() {
          @Override
          public void call(User authenticatedUser) {
            inputText.setUserInfo(authenticatedUser);
            Picasso.with(inputText.getContext())
                .load(authenticatedUser.getMiniProfileImageURLHttps())
                .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
                .tag(LOADINGTAG_TWEET_INPUT_ICON)
                .into(inputText.getIcon());
          }
        });
    inputText.addTextWatcher(textWatcher);
    inputText.setShortUrlLength(
        appSettings.getTwitterAPIConfig().getShortURLLengthHttps());
  }

  public void tearDownTweetInputView() {
    if (subscription != null && !subscription.isUnsubscribed()) {
      Picasso.with(getContext()).cancelTag(LOADINGTAG_TWEET_INPUT_ICON);
      subscription.unsubscribe();
    }
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
          ((TweetSendable) activity).observeUpdateStatus(observeUpdateStatus())
              .doOnTerminate(new Action0() {
                @Override
                public void call() {
                  v.setClickable(true);
                }
              }).subscribe(RequestWorkerBase.<Status>nopSubscriber());
        }
      };
    } else {
      return new View.OnClickListener() {
        @Override
        public void onClick(final View view) {
          view.setClickable(false);
          observeUpdateStatus().doOnTerminate(new Action0() {
            @Override
            public void call() {
              view.setClickable(true);
            }
          }).subscribe(RequestWorkerBase.<Status>nopSubscriber());
        }
      };
    }
  }

  private void tearDownSendTweetFab() {
    tweetSendFab.setOnClickListener(null);
  }

  private Observable<Status> createSendObservable() {
    final String sendingText = binding.mainTweetInputView.getText().toString();
    if (!isStatusUpdateNeeded()) {
      return statusRequestWorker.observeUpdateStatus(sendingText);
    }
    String s = sendingText;
    for (long q : quoteStatusIds) {
      final Status status = statusRequestWorker.getCache().find(q);
      if (status == null) {
        continue;
      }
      s += " https://twitter.com/" + status.getUser().getScreenName() + "/status/" + q;
    }
    final StatusUpdate statusUpdate = new StatusUpdate(s);
    if (replyEntity != null) {
      statusUpdate.setInReplyToStatusId(replyEntity.inReplyToStatusId);
    }
    return statusRequestWorker.observeUpdateStatus(statusUpdate);
  }

  private Observable<Status> observeUpdateStatus() {
    final TweetInputView inputText = binding.mainTweetInputView;
    return createSendObservable()
        .doOnNext(new Action1<Status>() {
          @Override
          public void call(Status status) {
            inputText.getText().clear();
            inputText.clearFocus();
            inputText.disappearing();
            setupMenuVisibility();
          }
        }).doOnCompleted(new Action0() {
          @Override
          public void call() {
            replyEntity = null;
            quoteStatusIds.clear();
          }
        });
  }

  private boolean isStatusUpdateNeeded() {
    return replyEntity != null
        || quoteStatusIds.size() > 0;
  }

  public void addQuoteStatus(long quoteStatusId) {
    quoteStatusIds.add(quoteStatusId);
  }

  public void collapseStatusInputView() {
    tearDownTweetInputView();
    tearDownSendTweetFab();
    replyEntity = null;
    quoteStatusIds.clear();
    binding.mainTweetInputView.disappearing();
    setupMenuVisibility();
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  interface TweetSendable {
    void setupInput(@TweetType int type, long statusId);

    Observable<Status> observeUpdateStatus(Observable<Status> updateStatusObservable);
  }

  public static final int TYPE_DEFAULT = 0;
  public static final int TYPE_REPLY = 1;
  public static final int TYPE_QUOTE = 2;
  public static final int TYPE_NONE = -1;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef(value = {TYPE_DEFAULT, TYPE_REPLY, TYPE_QUOTE, TYPE_NONE})
  public @interface TweetType {
  }

  private static class ReplyEntity {
    long inReplyToStatusId;
    Set<String> screenNames;

    static ReplyEntity create(@NonNull Status status) {
      final ReplyEntity res = new ReplyEntity();
      res.inReplyToStatusId = status.getId();
      res.screenNames = new LinkedHashSet<>();
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
      screenNames.add(screenName);
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