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

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
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
import android.view.inputmethod.InputMethodManager;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.AppSettingRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.squareup.picasso.Picasso;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.View.GONE;

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
  StatusRequestWorker statusRequestWorker;
  @Inject
  AppSettingRequestWorker appSettingRequestWorker;
  @Inject
  AppSettingStore appSettings;
  private Disposable subscription;

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

  private AppendMediaBottomSheet appendMediaBottomSheet;

  @Override
  public void onStart() {
    super.onStart();
    // workaround: for support lib 25.1.0
    binding.mainTweetInputView.setVisibility(GONE);

    statusCache.open();
    appSettings.open();
    final Bundle arguments = getArguments();
    final @TweetType int tweetType = arguments.getInt("tweet_type");
    final long statusId = arguments.getLong("status_id", -1);
    stretchTweetInputView(tweetType, statusId);
    binding.mainTweetInputView.getAppendImageButton().setOnClickListener(v -> {
      if (appendMediaBottomSheet == null) {
        appendMediaBottomSheet = new AppendMediaBottomSheet();
      }
      final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
      appendMediaBottomSheet.show(getFragmentManager(), "bottomSheet");
    });
  }

  @Override
  public void onStop() {
    super.onStop();
    appSettings.close();
    statusCache.close();
    binding.mainTweetInputView.getAppendImageButton().setOnClickListener(null);
  }

  private FloatingActionButton tweetSendFab;

  public void setTweetSendFab(FloatingActionButton fab) {
    this.tweetSendFab = fab;
  }

  private final TextWatcher textWatcher = new TextWatcher() {
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

  private final List<Long> quoteStatusIds = new ArrayList<>(4);

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

  @Inject
  TypedCache<Status> statusCache;

  private void stretchTweetInputViewWithInReplyTo(long inReplyToStatusId) {
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inReplyTo == null) {
      stretchTweetInputView();
      return;
    }

    appSettingRequestWorker.getAuthenticatedUser().subscribe(u -> {
      replyEntity = ReplyEntity.create(inReplyTo, u);
      inputText.addText(replyEntity.createReplyString());
      inputText.setInReplyTo();
      stretchTweetInputView();
    });
  }

  private void stretchTweetInputViewWithQuoteStatus(long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
    stretchTweetInputView();
  }

  private void setUpTweetInputView() {
    final TweetInputView inputText = binding.mainTweetInputView;
    subscription = appSettingRequestWorker.getAuthenticatedUser()
        .subscribe(authenticatedUser -> {
          inputText.setUserInfo(authenticatedUser);
          Picasso.with(inputText.getContext())
              .load(authenticatedUser.getMiniProfileImageURLHttps())
              .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
              .tag(LOADINGTAG_TWEET_INPUT_ICON)
              .into(inputText.getIcon());
        }, e -> Log.e(TAG, "setUpTweetInputView: ", e));
    inputText.addTextWatcher(textWatcher);
    inputText.setShortUrlLength(
        appSettings.getTwitterAPIConfig().getShortURLLengthHttps());
  }

  public void tearDownTweetInputView() {
    if (subscription != null && !subscription.isDisposed()) {
      Picasso.with(getContext()).cancelTag(LOADINGTAG_TWEET_INPUT_ICON);
      subscription.dispose();
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
      return v -> {
        v.setClickable(false);
        ((TweetSendable) activity).observeUpdateStatus(observeUpdateStatus())
            .subscribe((s, e) -> v.setClickable(true));
      };
    } else {
      return view -> {
        view.setClickable(false);
        observeUpdateStatus().subscribe((s, e) -> view.setClickable(true));
      };
    }
  }

  private void tearDownSendTweetFab() {
    tweetSendFab.setOnClickListener(null);
  }

  private Single<Status> createSendObservable() {
    final String sendingText = binding.mainTweetInputView.getText().toString();
    if (!isStatusUpdateNeeded()) {
      return statusRequestWorker.observeUpdateStatus(sendingText);
    }
    StringBuilder s = new StringBuilder(sendingText);
    for (long q : quoteStatusIds) {
      final Status status = statusCache.find(q);
      if (status == null) {
        continue;
      }
      s.append(" https://twitter.com/")
          .append(status.getUser().getScreenName()).append("/status/").append(q);
    }
    final StatusUpdate statusUpdate = new StatusUpdate(s.toString());
    if (replyEntity != null) {
      statusUpdate.setInReplyToStatusId(replyEntity.inReplyToStatusId);
    }
    return appendMediaBottomSheet != null && appendMediaBottomSheet.media.size() > 0 ?
        statusRequestWorker.observeUpdateStatus(getContext(), statusUpdate, appendMediaBottomSheet.media)
        : statusRequestWorker.observeUpdateStatus(statusUpdate);
  }

  private Single<Status> observeUpdateStatus() {
    final TweetInputView inputText = binding.mainTweetInputView;
    return createSendObservable().doOnSuccess(status -> {
      inputText.getText().clear();
      inputText.reset();
      inputText.clearFocus();
      inputText.disappearing();
      setupMenuVisibility();
      replyEntity = null;
      quoteStatusIds.clear();
      if (appendMediaBottomSheet != null) {
        appendMediaBottomSheet.media.clear();
      }
    });
  }

  private boolean isStatusUpdateNeeded() {
    return replyEntity != null
        || quoteStatusIds.size() > 0
        || (appendMediaBottomSheet != null && appendMediaBottomSheet.media.size() > 0);
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

    Single<Status> observeUpdateStatus(Single<Status> updateStatusObservable);
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

    static ReplyEntity create(@NonNull Status status, User from) {
      final ReplyEntity res = new ReplyEntity();
      res.inReplyToStatusId = status.getId();
      res.screenNames = new LinkedHashSet<>();
      final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
      for (UserMentionEntity u : userMentionEntities) {
        res.screenNames.add(u.getScreenName());
      }

      if (status.isRetweet()) {
        final Status retweetedStatus = status.getRetweetedStatus();
        res.screenNames.add(retweetedStatus.getUser().getScreenName());
      }

      final User user = status.getUser();
      res.screenNames.add(user.getScreenName());
      res.screenNames.remove(from.getScreenName());
      return res;
    }

    String createReplyString() {
      StringBuilder s = new StringBuilder();
      for (String sn : screenNames) {
        s.append("@").append(sn).append(" ");
      }
      return s.toString();
    }
  }

  public static class AppendMediaBottomSheet extends BottomSheetDialogFragment {
    private View takePicture;
    private View selectFromGallery;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final Dialog dialog = super.onCreateDialog(savedInstanceState);
      final View view = View.inflate(getContext(), R.layout.view_append_image_menu, null);
      takePicture = view.findViewById(R.id.appendMedia_take_picture);
      selectFromGallery = view.findViewById(R.id.appendMedia_select_picture);
      dialog.setContentView(view);
      dialog.setCancelable(true);
      dialog.setCanceledOnTouchOutside(true);
      return dialog;
    }

    @Override
    public void onStart() {
      super.onStart();
      selectFromGallery.setOnClickListener(v -> {
        final Intent intent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
          intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
          intent.setType("image/*");
          intent.addCategory(Intent.CATEGORY_OPENABLE);
        } else {
          intent = new Intent(Intent.ACTION_GET_CONTENT);
          intent.setType("image/*");
        }
        startActivityForResult(intent, 40);
      });
      takePicture.setOnClickListener(v -> {
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Media.CONTENT_TYPE, "image/jpeg");
        contentValues.put(Media.TITLE, "tw_" + System.currentTimeMillis() + ".jpg");

        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraPicUri = v.getContext().getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, contentValues);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri);
        startActivityForResult(intent, 40);
      });
    }

    @Override
    public void onStop() {
      super.onStop();
      takePicture.setOnClickListener(null);
      selectFromGallery.setOnClickListener(null);
    }

    private Uri cameraPicUri;
    private final List<Uri> media = new ArrayList<>(4);

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      Log.d(TAG, "onActivityResult: " + requestCode);
      if (requestCode == 40) {
        if (resultCode == RESULT_OK) {
          media.add(data == null ? cameraPicUri : data.getData());
        } else if (cameraPicUri != null) {
          getContext().getContentResolver().delete(cameraPicUri, null, null);
        }
        cameraPicUri = null;
        dismiss();
      }
      super.onActivityResult(requestCode, resultCode, data);
    }
  }
}