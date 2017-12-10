/*
 * Copyright (c) 2017. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.input;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.FragmentTweetInputBinding;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageQuery;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * TweetInputFragment provides TweetInputView and logic to send tweet.
 *
 * Created by akihit on 2016/02/06.
 */
public class TweetInputFragment extends Fragment {
  private static final String TAG = TweetInputFragment.class.getSimpleName();
  private static final int REQUEST_CODE_MEDIA_CHOOSER = 40;
  private static final int REQUEST_CODE_WRITE_EXTERNAL_PERMISSION = 50;
  private FragmentTweetInputBinding binding;
  @Inject
  TweetInputViewModel viewModel;
  private Disposable currentUserSubscription;
  private Disposable updateStatusTask;
  private Disposable iconSubs;

  public static TweetInputFragment create() {
    return new TweetInputFragment();
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
    getLifecycle().addObserver(viewModel);
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
      updateStatusTask = viewModel.createSendObservable(getContext(), binding.mainTweetInputView.getText().toString()).subscribe(s -> {
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
      MediaContainerPresenter mediaContainerPresenter
          = new MediaContainerPresenter(binding.mainTweetInputView.getMediaContainer(), viewModel);
      getLifecycle().addObserver(mediaContainerPresenter);
    }
    return binding.getRoot();
  }

  @Override
  public void onStart() {
    Log.d(TAG, "onStart: ");
    super.onStart();

    final TweetInputView inputText = binding.mainTweetInputView;
    inputText.getAppendImageButton().setOnClickListener(v -> {
      final Context context = v.getContext();
      final InputMethodManager imm = (InputMethodManager) context.getSystemService(INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
      }

      if (isPermissionNeed(context)) {
        requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_EXTERNAL_PERMISSION);
      } else {
        showMediaChooser();
      }
    });
    inputText.addTextWatcher(textWatcher);
  }

  @Override
  public void onResume() {
    super.onResume();
    changeCurrentUser();
  }

  @Override
  public void onStop() {
    Log.d(TAG, "onStop: ");
    super.onStop();
    Utils.maybeDispose(currentUserSubscription);
    Utils.maybeDispose(iconSubs);
    binding.mainTweetInputView.getAppendImageButton().setOnClickListener(null);
    binding.mainTweetInputView.removeTextWatcher(textWatcher);
  }

  @Override
  public void onDetach() {
    super.onDetach();
    Utils.maybeDispose(updateStatusTask);
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

  public boolean isNewTweetCreatable() {
    return !isTweetInputViewVisible()
        && !Utils.isSubscribed(updateStatusTask)
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

  private void setupReplyEntity(long inReplyToStatusId) {
    viewModel.setReplyToStatusId(inReplyToStatusId);
    if (viewModel.hasReplyEntity()) {
      binding.mainTweetInputView.addText(viewModel.createReplyString());
      binding.mainTweetInputView.setInReplyTo();
    }
  }

  private void setupQuote(long quotedStatus) {
    viewModel.addQuoteId(quotedStatus);
    binding.mainTweetInputView.setQuote();
  }

  private void expandTweetInputView() {
    binding.mainTweetInputView.setShortUrlLength(viewModel.getUrlLength());
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
    viewModel.clear();
    binding.mainTweetInputView.reset();
  }

  private boolean isCleared() {
    return viewModel.isCleared()
        && (binding == null || binding.mainTweetInputView.getText().length() <= 0);
  }

  public boolean isTweetInputViewVisible() {
    return binding.mainTweetInputView.isVisible();
  }

  public void changeCurrentUser() {
    Utils.maybeDispose(currentUserSubscription);
    final TweetInputView inputText = binding.mainTweetInputView;
    currentUserSubscription = viewModel.observeCurrentUser()
        .subscribe(inputText::setUserInfo, e -> Log.e(TAG, "setUpTweetInputView: ", e));

    Utils.maybeDispose(iconSubs);
    iconSubs = viewModel.observeCurrentUser().map(currentUser ->
        new ImageQuery.Builder(currentUser.getMiniProfileImageURLHttps())
            .sizeForSquare(getContext(), R.dimen.small_user_icon)
            .placeholder(getContext(), R.drawable.ic_person_outline_black)
            .build())
        .flatMap(viewModel::queryImage)
        .subscribe(d -> inputText.getIcon().setImageDrawable(d), th -> {});
  }

  public interface TweetInputListener {
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

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.d(TAG, "onActivityResult: " + requestCode);
    if (requestCode == REQUEST_CODE_MEDIA_CHOOSER) {
      viewModel.onMediaChooserResult(getContext(), resultCode, data);
    }
    super.onActivityResult(requestCode, resultCode, data);
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
    final Intent chooser = viewModel.getMediaChooserIntent(getContext());
    startActivityForResult(chooser, REQUEST_CODE_MEDIA_CHOOSER);
  }

  private static final String SS_TWEET_INPUT_VIEW_VISIBILITY = "ss_tweetInputView.visibility";

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState: ");
    super.onSaveInstanceState(outState);
    outState.putInt(SS_TWEET_INPUT_VIEW_VISIBILITY, binding.mainTweetInputView.getVisibility());
    viewModel.onSaveInstanceState(outState);
  }

  @Override
  public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
    super.onViewStateRestored(savedInstanceState);
    if (savedInstanceState == null) {
      return;
    }
    final int visibility = savedInstanceState.getInt(SS_TWEET_INPUT_VIEW_VISIBILITY);
    binding.mainTweetInputView.setVisibility(visibility);

    viewModel.onViewStateRestored(savedInstanceState);
    if (viewModel.hasQuoteStatus()) {
      binding.mainTweetInputView.setQuote();
    }
    if (viewModel.hasReplyEntity()) {
      binding.mainTweetInputView.setInReplyTo();
    }
    setupMenuVisibility();
  }
}