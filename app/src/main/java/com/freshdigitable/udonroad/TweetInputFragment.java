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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.MediaStore.Images.Media;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.TypedCache;
import com.freshdigitable.udonroad.media.ThumbnailContainer;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageRepository;
import com.freshdigitable.udonroad.subscriber.StatusRequestWorker;

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
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.TwitterAPIConfiguration;
import twitter4j.User;
import twitter4j.UserMentionEntity;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * TweetInputFragment provides TweetInputView and logic to send tweet.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetInputFragment extends Fragment {
  private static final String TAG = TweetInputFragment.class.getSimpleName();
  private static final String LOADINGTAG_TWEET_INPUT_ICON = "TweetInputIcon";
  private static final int REQUEST_CODE_MEDIA_CHOOSER = 40;
  private static final int REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 50;
  private FragmentTweetInputBinding binding;
  @Inject
  StatusRequestWorker statusRequestWorker;
  @Inject
  AppSettingStore appSettings;
  @Inject
  TypedCache<Status> statusCache;
  private Disposable currentUserSubscription;
  private Disposable updateStatusTask;
  @Inject
  ImageRepository imageRepository;
  private Disposable iconSubs;

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

  private final SparseArray<MenuItem> menuItems = new SparseArray<>();

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    Log.d(TAG, "onCreateOptionsMenu: ");
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.tweet_input, menu);
    for (@IdRes int resId : new int[]{R.id.action_writeTweet, R.id.action_sendTweet, R.id.action_resumeTweet}) {
      menuItems.put(resId, menu.findItem(resId));
    }
    setupMenuVisibility();
  }

  private void setupMenuVisibility() {
    if (binding == null) {
      return;
    }
    if (binding.mainTweetInputView.isVisible()) {
      setupMenuVisibility(R.id.action_sendTweet);
    } else {
      setupMenuVisibility(isCleared() ?
          R.id.action_writeTweet : R.id.action_resumeTweet);
    }
  }

  private void setupMenuVisibility(@IdRes int visibleItemId) {
    for (int i = 0; i < menuItems.size(); i++) {
      final MenuItem item = menuItems.valueAt(i);
      item.setVisible(item.getItemId() == visibleItemId);
      setupItemEnable(item);
    }
  }

  private void setupItemEnable(@NonNull MenuItem item) {
    final int itemId = item.getItemId();
    if (!item.isVisible()) {
      return;
    }
    if (itemId == R.id.action_writeTweet) {
      item.setEnabled(isCleared());
    } else if (itemId == R.id.action_sendTweet) {
      item.setEnabled(binding.mainTweetInputView.getText().length() > 0);
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    Log.d(TAG, "onOptionsItemSelected: ");
    final int itemId = item.getItemId();
    if (itemId == R.id.action_sendTweet) {
      menuItems.get(R.id.action_sendTweet).setEnabled(false);
      final TweetInputListener tweetInputListener = getTweetInputListener();
      updateStatusTask = createSendObservable().subscribe(s -> {
        tweetInputListener.onSendCompleted();
        clear();
        setupMenuVisibility(R.id.action_writeTweet);
      }, e ->
          setupMenuVisibility(R.id.action_resumeTweet));
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
    appSettings.open();
    changeCurrentUser();

    final TweetInputView inputText = binding.mainTweetInputView;
    inputText.getAppendImageButton().setOnClickListener(v -> {
      final Context context = v.getContext();
      final InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
      }

      if (isPermissionNeed(context)) {
        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_PERMISSION);
        return;
      }
      showMediaChooser();
    });
    inputText.addTextWatcher(textWatcher);
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    if (currentUserSubscription != null && !currentUserSubscription.isDisposed()) {
      currentUserSubscription.dispose();
    }
    if (iconSubs != null && !iconSubs.isDisposed()) {
      iconSubs.dispose();
    }
    appSettings.close();
    binding.mainTweetInputView.getAppendImageButton().setOnClickListener(null);
    binding.mainTweetInputView.removeTextWatcher(textWatcher);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    if (updateStatusTask != null && !updateStatusTask.isDisposed()) {
      updateStatusTask.dispose();
    }
    menuItems.clear();
  }

  private final TextWatcher textWatcher = new TextWatcher() {
    @Override
    public void afterTextChanged(Editable editable) {
      final MenuItem sendTweetItem = menuItems.get(R.id.action_sendTweet);
      if (sendTweetItem != null) {
        sendTweetItem.setEnabled(editable.length() >= 1);
      }
    }

    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
    @Override
    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
  };

  private final List<Long> quoteStatusIds = new ArrayList<>(4);

  public boolean isNewTweetCreatable() {
    return !isTweetInputViewVisible()
        && (updateStatusTask == null || updateStatusTask.isDisposed())
        && isCleared();
  }

  public void expandForResume() {
    if (isCleared()) {
      throw new IllegalStateException("there is no tweet for resume...");
    }
    expandTweetInputView();
  }

  public void expandTweetInputView(@TweetType int type, long statusId) {
    if (type == TYPE_NONE || !isNewTweetCreatable()) {
      return;
    }
    if (type == TYPE_REPLY) {
      setupReplyEntity(statusId);
    } else if (type == TYPE_QUOTE) {
      setupQuote(statusId);
    }
    expandTweetInputView();
  }

  private ReplyEntity replyEntity;

  private void setupReplyEntity(long inReplyToStatusId) {
    statusCache.open();
    final Status inReplyTo = statusCache.find(inReplyToStatusId);
    if (inReplyTo != null) {
      replyEntity = ReplyEntity.create(inReplyTo, appSettings.getCurrentUserId());
      binding.mainTweetInputView.addText(replyEntity.createReplyString());
      binding.mainTweetInputView.setInReplyTo();
    }
    statusCache.close();
  }

  private void setupQuote(long quotedStatus) {
    quoteStatusIds.add(quotedStatus);
    binding.mainTweetInputView.setQuote();
  }

  private void expandTweetInputView() {
    final TwitterAPIConfiguration twitterAPIConfig = appSettings.getTwitterAPIConfig();
    if (twitterAPIConfig != null) {
      binding.mainTweetInputView.setShortUrlLength(twitterAPIConfig.getShortURLLengthHttps());
    }
    setupExpandAnimation(binding.mainTweetInputView);
    setupMenuVisibility(R.id.action_sendTweet);
  }

  private static final int ANIM_DURATION = 200;
  private static final FastOutSlowInInterpolator ANIM_INTERPOLATOR = new FastOutSlowInInterpolator();
  private ValueAnimator valueAnimator;
  private ViewTreeObserver.OnPreDrawListener callback;

  private ViewTreeObserver.OnPreDrawListener getCallback(TweetInputView view) {
    return () -> {
      final int h = view.getHeight();
      if (h <= 0) {
        return true;
      }
      animateToExpand(view);
      view.getViewTreeObserver().removeOnPreDrawListener(callback);
      return true;
    };
  }

  private void setupExpandAnimation(@NonNull TweetInputView view) {
    if (valueAnimator != null && valueAnimator.isRunning()) {
      valueAnimator.cancel();
    }
    view.measure(
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    final int height = view.getMeasuredHeight();
    if (height > 0) {
      view.setVisibility(View.VISIBLE);
      animateToExpand(view);
    } else {
      callback = getCallback(view);
      view.getViewTreeObserver().addOnPreDrawListener(callback);
      view.setVisibility(View.VISIBLE);
    }
  }

  private void animateToExpand(@NonNull TweetInputView view) {
    final int height = view.getMeasuredHeight();
    final View container = (View) view.getParent();
    final ValueAnimator valueAnimator = ValueAnimator.ofInt(-height, 0);
    valueAnimator.setDuration(ANIM_DURATION);
    valueAnimator.setInterpolator(ANIM_INTERPOLATOR);
    valueAnimator.addUpdateListener(animation -> {
      final int animatedFraction = (int) animation.getAnimatedValue();
      view.setTranslationY(animatedFraction);
      container.getLayoutParams().height = height + animatedFraction;
      container.requestLayout();
    });
    valueAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        view.setTranslationY(0);
        view.show();
        container.getLayoutParams().height = WRAP_CONTENT;
        container.requestLayout();
      }
    });
    this.valueAnimator = valueAnimator;
    this.valueAnimator.start();
  }

  private Single<Status> createSendObservable() {
    final String sendingText = binding.mainTweetInputView.getText().toString();
    if (!isStatusUpdateNeeded()) {
      return statusRequestWorker.observeUpdateStatus(sendingText);
    }
    StringBuilder s = new StringBuilder(sendingText);
    statusCache.open();
    for (long q : quoteStatusIds) {
      final Status status = statusCache.find(q);
      if (status == null) {
        continue;
      }
      s.append(" https://twitter.com/")
          .append(status.getUser().getScreenName()).append("/status/").append(q);
    }
    statusCache.close();
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

  public void collapseStatusInputView() {
    animateToCollapse(binding.mainTweetInputView);
  }

  private void animateToCollapse(@NonNull TweetInputView view) {
    if (valueAnimator != null && valueAnimator.isRunning()) {
      valueAnimator.cancel();
    }
    final View container = (View) view.getParent();
    final int height = view.getHeight();
    final ValueAnimator valueAnimator = ValueAnimator.ofInt(0, -height);
    valueAnimator.setInterpolator(ANIM_INTERPOLATOR);
    valueAnimator.setDuration(ANIM_DURATION);
    valueAnimator.addUpdateListener(animation -> {
      final int animatedValue = (int) animation.getAnimatedValue();
      view.setTranslationY(animatedValue);
      container.getLayoutParams().height = height + animatedValue;
      container.requestLayout();
    });
    valueAnimator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        view.hide();
        container.getLayoutParams().height = WRAP_CONTENT;
        container.requestLayout();
      }
    });
    this.valueAnimator = valueAnimator;
    this.valueAnimator.start();
  }

  public void cancelInput() {
    clear();
    collapseStatusInputView();
    setupMenuVisibility(R.id.action_writeTweet);
  }

  private void clear() {
    replyEntity = null;
    quoteStatusIds.clear();
    clearMedia();
    binding.mainTweetInputView.reset();
  }

  private boolean isCleared() {
    return replyEntity == null && quoteStatusIds.isEmpty() && media.isEmpty()
        && (binding == null || binding.mainTweetInputView.getText().length() <= 0);
  }

  public boolean isTweetInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  public void changeCurrentUser() {
    if (currentUserSubscription != null && !currentUserSubscription.isDisposed()) {
      currentUserSubscription.dispose();
    }
    final TweetInputView inputText = binding.mainTweetInputView;
    currentUserSubscription = appSettings.observeCurrentUser().subscribe(currentUser -> {
      inputText.setUserInfo(currentUser);
      if (iconSubs != null && !iconSubs.isDisposed()) {
        iconSubs.dispose();
      }
      iconSubs = imageRepository.querySmallUserIcon(currentUser, LOADINGTAG_TWEET_INPUT_ICON)
          .subscribe(d -> inputText.getIcon().setImageDrawable(d));
    }, e -> Log.e(TAG, "setUpTweetInputView: ", e));
  }

  interface TweetInputListener {
    void onSendCompleted();
  }

  private final TweetInputListener emptyListener = () -> {};

  @NonNull
  private TweetInputListener getTweetInputListener() {
    final FragmentActivity activity = getActivity();
    return activity instanceof TweetInputListener ? (TweetInputListener) activity
        : emptyListener;
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
    menuItems.get(R.id.action_sendTweet).setEnabled(true);
  }

  private void removeMedia(Uri uri) {
    media.remove(uri);
    updateMediaContainer();
    menuItems.get(R.id.action_sendTweet).setEnabled(!media.isEmpty());
  }

  private void clearMedia() {
    media.clear();
    Utils.maybeDispose(uploadedMediaSubs);
    binding.mainTweetInputView.clearMedia();
  }

  private CompositeDisposable uploadedMediaSubs;

  private void updateMediaContainer() {
    Utils.maybeDispose(uploadedMediaSubs);

    uploadedMediaSubs = new CompositeDisposable();
    final ThumbnailContainer mediaContainer = binding.mainTweetInputView.getMediaContainer();
    mediaContainer.bindMediaEntities(media.size());
    final int thumbCount = mediaContainer.getThumbCount();
    for (int i = 0; i < thumbCount; i++) {
      final Uri uri = media.get(i);
      final ImageView imageView = (ImageView) mediaContainer.getChildAt(i);
      final Disposable d = imageRepository.queryToFit(uri, imageView, true, "upload_media")
          .subscribe(imageView::setImageDrawable);
      uploadedMediaSubs.add(d);

      imageView.setOnCreateContextMenuListener((contextMenu, view, contextMenuInfo) -> {
        final MenuItem delete = contextMenu.add(0, 1, 0, R.string.media_upload_delete);
        delete.setOnMenuItemClickListener(menuItem -> {
          removeMedia(uri);
          return true;
        });
      });
    }
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
    if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_PERMISSION) {
      showMediaChooser();
    }
  }

  private void showMediaChooser() {
    final Intent pickMediaIntent = getPickMediaIntent();
    final Intent cameraIntent = getCameraIntent(getContext());
    cameraPicUri = cameraIntent.getParcelableExtra(MediaStore.EXTRA_OUTPUT);
    final Intent chooser = Intent.createChooser(pickMediaIntent, getString(R.string.media_chooser_title));
    chooser.putExtra(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
            Intent.EXTRA_ALTERNATE_INTENTS : Intent.EXTRA_INITIAL_INTENTS,
        new Intent[]{cameraIntent});
    startActivityForResult(chooser, REQUEST_CODE_MEDIA_CHOOSER);
  }

  private static final String SS_MEDIA = "ss_media";
  private static final String SS_QUOTED_STATUS_IDS = "ss_quotedStatusIds";
  private static final String SS_CAMERA_PIC_URI = "ss_cameraPicUri";
  private static final String SS_REPLIED_STATUS_ID = "ss_repliedStatusId";
  private static final String SS_REPLIED_USER_NAMES = "ss_repliedUserNames";
  private static final String SS_TWEET_INPUT_VIEW_VISIBILITY = "ss_tweetInputView.visibility";

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState: ");
    super.onSaveInstanceState(outState);
    outState.putInt(SS_TWEET_INPUT_VIEW_VISIBILITY, binding.mainTweetInputView.getVisibility());
    outState.putParcelableArray(SS_MEDIA, media.toArray(new Uri[media.size()]));
    final long[] qIds = new long[quoteStatusIds.size()];
    for (int i = 0; i < quoteStatusIds.size(); i++) {
      qIds[i] = quoteStatusIds.get(i);
    }
    outState.putLongArray(SS_QUOTED_STATUS_IDS, qIds);
    outState.putParcelable(SS_CAMERA_PIC_URI, cameraPicUri);
    if (replyEntity != null) {
      outState.putLong(SS_REPLIED_STATUS_ID, replyEntity.inReplyToStatusId);
      outState.putStringArray(SS_REPLIED_USER_NAMES,
          replyEntity.screenNames.toArray(new String[replyEntity.screenNames.size()]));
    }
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState == null) {
      return;
    }
    final int visibility = savedInstanceState.getInt(SS_TWEET_INPUT_VIEW_VISIBILITY);
    binding.mainTweetInputView.setVisibility(visibility);
    final Parcelable[] uris = savedInstanceState.getParcelableArray(SS_MEDIA);
    if (uris != null && uris.length > 0) {
      for (Parcelable p : uris) {
        media.add((Uri) p);
      }
    }

    final long[] qIds = savedInstanceState.getLongArray(SS_QUOTED_STATUS_IDS);
    if (qIds != null) {
      for (long qId : qIds) {
        quoteStatusIds.add(qId);
      }
    }
    if (!quoteStatusIds.isEmpty()) {
      binding.mainTweetInputView.setQuote();
    }

    cameraPicUri = savedInstanceState.getParcelable(SS_CAMERA_PIC_URI);

    final long repliedStatusId = savedInstanceState.getLong(SS_REPLIED_STATUS_ID, -1);
    if (repliedStatusId != -1) {
      final String[] repliedUserNames = savedInstanceState.getStringArray(SS_REPLIED_USER_NAMES);
      final LinkedHashSet<String> userNames = new LinkedHashSet<>();
      if (repliedUserNames != null) {
        Collections.addAll(userNames, repliedUserNames);
      }
      replyEntity = new ReplyEntity(repliedStatusId, userNames);
    }
    if (replyEntity != null) {
      binding.mainTweetInputView.setInReplyTo();
    }

    setupMenuVisibility();
  }
}