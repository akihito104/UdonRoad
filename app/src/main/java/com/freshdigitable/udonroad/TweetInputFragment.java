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

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
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
import android.widget.ImageView;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.subscriber.AppSettingRequestWorker;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * TweetInputFragment provides TweetInputView and logic to send tweet.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetInputFragment extends Fragment {
  private static final String TAG = TweetInputFragment.class.getSimpleName();
  private static final String LOADINGTAG_TWEET_INPUT_ICON = "TweetInputIcon";
  private static final int REQUEST_CODE_MEDIA_CHOOSER = 40;
  private static final int REQUEST_CODE_WRITE_EXTERN_PERMISSION = 50;
  private FragmentTweetInputBinding binding;
  @Inject
  StatusRequestWorker statusRequestWorker;
  @Inject
  AppSettingRequestWorker appSettingRequestWorker;
  @Inject
  AppSettingStore appSettings;
  @Inject
  TypedCache<Status> statusCache;
  private Disposable subscription;
  private Disposable updateStatusTask;

  public static TweetInputFragment create() {
    return new TweetInputFragment();
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
    if (binding == null) {
      return;
    }
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
    if (binding == null) {
      binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tweet_input, container, false);
    }
    return binding.getRoot();
  }

  @Override
  public void onStart() {
    Log.d(TAG, "onStart: ");
    super.onStart();
    statusCache.open();
    appSettings.open();

    final TweetInputView inputText = binding.mainTweetInputView;
    subscription = appSettings.observeCurrentUser().subscribe(authenticatedUser -> {
      inputText.setUserInfo(authenticatedUser);
      Picasso.with(inputText.getContext())
          .load(authenticatedUser.getMiniProfileImageURLHttps())
          .resizeDimen(R.dimen.small_user_icon, R.dimen.small_user_icon)
          .tag(LOADINGTAG_TWEET_INPUT_ICON)
          .into(inputText.getIcon());
    }, e -> Log.e(TAG, "setUpTweetInputView: ", e));

    inputText.getAppendImageButton().setOnClickListener(v -> {
      final InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

      if (isPermissionNeed(v.getContext())) {
        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERN_PERMISSION);
        return;
      }
      showMediaChooser(v.getContext());
    });
    inputText.addTextWatcher(textWatcher);
    tweetSendFab.setOnClickListener(createSendClickListener());
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    if (subscription != null && !subscription.isDisposed()) {
      Picasso.with(getContext()).cancelTag(LOADINGTAG_TWEET_INPUT_ICON);
      subscription.dispose();
    }
    appSettings.close();
    statusCache.close();
    binding.mainTweetInputView.getAppendImageButton().setOnClickListener(null);
    binding.mainTweetInputView.removeTextWatcher(textWatcher);
    tweetSendFab.setOnClickListener(null);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    if (updateStatusTask != null && !updateStatusTask.isDisposed()) {
      updateStatusTask.dispose();
    }
    tweetSendFab.setOnClickListener(null);
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
    if (type == TYPE_REPLY) {
      setupReplyEntity(statusId);
    } else if (type == TYPE_QUOTE) {
      setupQuote(statusId);
    }
    stretchTweetInputView();
  }

  private ReplyEntity replyEntity;

  private void setupReplyEntity(long inReplyToStatusId) {
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    if (inReplyTo != null) {
      replyEntity = ReplyEntity.create(inReplyTo, appSettings.getCurrentUserId());
      binding.mainTweetInputView.addText(replyEntity.createReplyString());
      binding.mainTweetInputView.setInReplyTo();
    }
  }

  private void setupQuote(long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
  }

  private void stretchTweetInputView() {
    setUpTweetInputView();
    setUpTweetSendFab();
    binding.mainTweetInputView.appearing();
    setupMenuVisibility();
  }

  private void setUpTweetInputView() {
    appSettingRequestWorker.verifyCredentials();
    binding.mainTweetInputView.setShortUrlLength(
        appSettings.getTwitterAPIConfig().getShortURLLengthHttps());
  }

  private void setUpTweetSendFab() {
    final TweetInputView inputText = binding.mainTweetInputView;
    if (inputText.getText().length() < 1) {
      tweetSendFab.setEnabled(false);
    }
  }

  private View.OnClickListener createSendClickListener() {
    final TweetSendable tweetSendable = getActivity() instanceof TweetSendable ?
        (TweetSendable) getActivity() : (s) -> {};
    final TweetInputView mainTweetInputView = binding.mainTweetInputView;
    return v -> {
      v.setClickable(false);
      mainTweetInputView.setClickable(false);
      updateStatusTask = createSendObservable().subscribe((s, e) -> {
        v.setClickable(true);
        mainTweetInputView.setClickable(true);
        if (s != null) { //  on success
          reset();
          tweetSendable.onTweetComplete(s);
        }
      });
    };
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
    return media.size() > 0 ?
        statusRequestWorker.observeUpdateStatus(getContext(), statusUpdate, media)
        : statusRequestWorker.observeUpdateStatus(statusUpdate);
  }

  private boolean isStatusUpdateNeeded() {
    return replyEntity != null
        || quoteStatusIds.size() > 0
        || media.size() > 0;
  }

  public void addQuoteStatus(long quoteStatusId) {
    quoteStatusIds.add(quoteStatusId);
  }

  public void collapseStatusInputView() {
    reset();
  }

  private void reset() {
    replyEntity = null;
    quoteStatusIds.clear();
    clearMedia();
    binding.mainTweetInputView.reset();
    binding.mainTweetInputView.disappearing();
    setupMenuVisibility();
  }

  public boolean isStatusInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  interface TweetSendable {
    void onTweetComplete(Status updated);
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
    final long inReplyToStatusId;
    final Set<String> screenNames;

    static ReplyEntity create(@NonNull Status status, long fromUserId) {
      final Set<String> screenNames = new LinkedHashSet<>();
      final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
      for (UserMentionEntity u : userMentionEntities) {
        if (u.getId() != fromUserId) {
          screenNames.add(u.getScreenName());
        }
      }

      if (status.isRetweet()) {
        final Status retweetedStatus = status.getRetweetedStatus();
        final User user = retweetedStatus.getUser();
        if (user.getId() != fromUserId) {
          screenNames.add(user.getScreenName());
        }
      }

      final User user = status.getUser();
      if (user.getId() != fromUserId) {
        screenNames.add(user.getScreenName());
      }
      return new ReplyEntity(status.getId(), screenNames);
    }

    private ReplyEntity(long replyToStatusId, Set<String> replyToUsers) {
      this.inReplyToStatusId = replyToStatusId;
      this.screenNames = replyToUsers;
    }

    String createReplyString() {
      StringBuilder s = new StringBuilder();
      for (String sn : screenNames) {
        s.append("@").append(sn).append(" ");
      }
      return s.toString();
    }
  }

  private Uri cameraPicUri;
  private final List<Uri> media = new ArrayList<>(4);

  private void addMedia(Uri uri) {
    if (uri == null) {
      return;
    }
    addAllMedia(Collections.singletonList(uri));
  }

  private void addAllMedia(Collection<Uri> uris) {
    media.addAll(uris);
    updateMediaContainer();
    tweetSendFab.setEnabled(true);
  }

  private void removeMedia(Uri uri) {
    media.remove(uri);
    updateMediaContainer();
    tweetSendFab.setEnabled(!media.isEmpty());
  }

  private void updateMediaContainer() {
    final ThumbnailContainer mediaContainer = binding.mainTweetInputView.getMediaContainer();
    mediaContainer.bindMediaEntities(media.size());
    final int thumbCount = mediaContainer.getThumbCount();
    for (int i = 0; i < thumbCount; i++) {
      final Uri uri = media.get(i);
      final RequestCreator rc = Picasso.with(getContext())
          .load(uri);
      if (mediaContainer.getThumbWidth() <= 0 || mediaContainer.getHeight() <= 0) {
        rc.fit();
      } else {
        rc.resize(mediaContainer.getThumbWidth(), mediaContainer.getHeight());
      }
      final ImageView imageView = (ImageView) mediaContainer.getChildAt(i);
      rc.centerCrop().into(imageView);

      imageView.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
        final MenuItem delete = contextMenu.add(0, 1, 0, "削除する");
        delete.setOnMenuItemClickListener(menuItem -> {
          removeMedia(uri);
          return true;
        });
      });
    }
  }

  private void clearMedia() {
    media.clear();
    binding.mainTweetInputView.clearMedia();
    tweetSendFab.setEnabled(false);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult: " + requestCode);
    if (requestCode == REQUEST_CODE_MEDIA_CHOOSER) {
      if (resultCode == RESULT_OK) {
        final List<Uri> uris = parseMediaData(data);
        if (uris.isEmpty()) {
          addMedia(cameraPicUri);
        } else {
          addAllMedia(uris);
          removeFromContentResolver(getContext(), cameraPicUri);
        }
      } else {
        removeFromContentResolver(getContext(), cameraPicUri);
      }
      cameraPicUri = null;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  private List<Uri> parseMediaData(Intent data) {
    if (data != null && data.getData() != null) {
      return Collections.singletonList(data.getData());
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
      return parseClipData(data);
    }
    return Collections.emptyList();
  }

  @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
  private static List<Uri> parseClipData(Intent data) {
    if (data == null || data.getClipData() == null) {
      return Collections.emptyList();
    }
    final ClipData clipData = data.getClipData();
    final int itemCount = clipData.getItemCount();
    final ArrayList<Uri> res = new ArrayList<>(itemCount);
    for (int i = 0; i < itemCount; i++) {
      final ClipData.Item item = clipData.getItemAt(i);
      res.add(item.getUri());
    }
    return res;
  }

  @NonNull
  private static Intent getPickMediaIntent() {
    final Intent intent;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
    } else {
      intent = new Intent(Intent.ACTION_GET_CONTENT);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
    }
    intent.setType("image/*");
    return intent;
  }

  @NonNull
  private static Intent getCameraIntent(Context context) {
    if (isPermissionNeed(context)) {
      return new Intent();
    }
    final ContentValues contentValues = new ContentValues();
    contentValues.put(Media.MIME_TYPE, "image/jpeg");
    contentValues.put(Media.TITLE, System.currentTimeMillis() + ".jpg");
    contentValues.put(Media.DISPLAY_NAME, System.currentTimeMillis() + ".jpg");

    final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    Uri cameraPicUri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, contentValues);
    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri);
    return intent;
  }

  private static void removeFromContentResolver(@NonNull Context context, Uri cameraPicUri) {
    if (cameraPicUri == null) {
      return;
    }
    context.getContentResolver().delete(cameraPicUri, null, null);
  }

  private static boolean isPermissionNeed(Context context) {
    return ActivityCompat.checkSelfPermission(context, WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_CODE_WRITE_EXTERN_PERMISSION) {
      showMediaChooser(getContext());
    }
  }

  private void showMediaChooser(Context context) {
    final Intent pickMediaIntent = getPickMediaIntent();
    final Intent cameraIntent = getCameraIntent(context);
    cameraPicUri = cameraIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    final Intent chooser = Intent.createChooser(pickMediaIntent, "添付する画像…");
    chooser.putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
            Intent.EXTRA_ALTERNATE_INTENTS : Intent.EXTRA_INITIAL_INTENTS,
        new Intent[]{cameraIntent});
    startActivityForResult(chooser, REQUEST_CODE_MEDIA_CHOOSER);
  }
}