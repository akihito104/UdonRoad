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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;

import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.SnackbarCapable;
import com.freshdigitable.udonroad.Utils;
import com.freshdigitable.udonroad.databinding.ActivityTweetInputBinding;
import com.freshdigitable.udonroad.databinding.ViewAccountSpinnerBinding;
import com.freshdigitable.udonroad.listitem.TwitterCombinedName;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.repository.ImageQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.disposables.Disposable;
import twitter4j.User;

/**
 * Created by akihit on 2017/12/03.
 */

public class TweetInputActivity extends AppCompatActivity implements SnackbarCapable {
  private ActivityTweetInputBinding binding;
  @Inject
  TweetInputViewModel viewModel;
  private TweetSendPresenter tweetSendPresenter;
  private ArrayAdapter<User> adapter;
  private Disposable modelSubs;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_tweet_input);
    InjectionUtil.getComponent(this).inject(this);

    getLifecycle().addObserver(viewModel);
    final MediaContainerPresenter mediaContainerPresenter
        = new MediaContainerPresenter(binding.tweetInputImageContainer, viewModel);
    getLifecycle().addObserver(mediaContainerPresenter);
    final LayoutInflater inflater = LayoutInflater.from(this);
    adapter = new ArrayAdapter<User>(this, R.layout.view_account_spinner) {
      private final Map<String, Disposable> iconSubsMap = new HashMap<>();

      @NonNull
      @Override
      public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
      }

      private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final ViewAccountSpinnerBinding accountBinding = convertView == null ?
            DataBindingUtil.inflate(inflater, R.layout.view_account_spinner, parent, false)
            : DataBindingUtil.bind(convertView);
        final User item = getItem(position);
        if (item == null) {
          throw new IllegalStateException();
        }
        accountBinding.accountSpinnerName.setNames(new TwitterCombinedName(item));
        final String miniProfileImageURLHttps = item.getMiniProfileImageURLHttps();
        final Disposable d = iconSubsMap.remove(miniProfileImageURLHttps);
        Utils.maybeDispose(d);

        final ImageQuery query = new ImageQuery.Builder(miniProfileImageURLHttps)
            .sizeForSquare(getContext(), R.dimen.small_user_icon)
            .placeholder(getContext(), R.drawable.ic_person_outline_black)
            .build();
        final Disposable iconSubs = viewModel.queryImage(query)
            .subscribe(accountBinding.accountSpinnerIcon::setImageDrawable);
        iconSubsMap.put(miniProfileImageURLHttps, iconSubs);
        return accountBinding.getRoot();
      }

      @Override
      public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
      }

      @Override
      public void clear() {
        super.clear();
        for (Map.Entry<String, Disposable> entry : iconSubsMap.entrySet()) {
          Utils.maybeDispose(entry.getValue());
        }
        iconSubsMap.clear();
      }
    };
    binding.tweetInputAccount.setAdapter(adapter);

    final String text = parseShareText();
    viewModel.setText(text);
  }

  private String parseShareText() {
    final Intent intent = getIntent();
    if (intent == null) {
      return "";
    }
    Log.d(getClass().getSimpleName(), "parseShareText: " + intent.toString());
    if (Intent.ACTION_SEND.equals(intent.getAction())) {
      return intent.getStringExtra(Intent.EXTRA_TEXT);
    }
    return "";
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    tweetSendPresenter = new TweetSendPresenter(menu, getMenuInflater(), viewModel);
    viewModel.setState(TweetInputModel.State.WRITING);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.action_sendTweet) {
      tweetSendPresenter.onSendTweetClicked(this, s -> {
        viewModel.clear();
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
          imm.hideSoftInputFromInputMethod(binding.tweetInputText.getWindowToken(), 0);
        }
        finish();
      }, th -> {});
      return true;
    }
    return false;
  }

  private MediaChooserController mediaChooserController = new MediaChooserController();

  @Override
  protected void onStart() {
    super.onStart();
    binding.tweetInputAddImage.setOnClickListener(v ->
        mediaChooserController.switchSoftKeyboardToMediaChooser(v, this));
    binding.tweetInputText.addTextChangedListener(viewModel.textWatcher);
  }

  @SuppressLint("SetTextI18n")
  @Override
  protected void onResume() {
    super.onResume();
    final List<? extends User> users = viewModel.getAllAuthenticatedUsers();
    adapter.addAll(users);
    modelSubs = viewModel.observeModel()
        .subscribe(model -> {
          if (!binding.tweetInputText.getText().toString().equals(model.getText())) {
            binding.tweetInputText.setText(model.getText());
          }
          binding.tweetInputCount.setText(Integer.toString(model.getRemainCount()));
        });
  }

  @Override
  protected void onPause() {
    super.onPause();
    adapter.clear();
    Utils.maybeDispose(modelSubs);
  }

  @Override
  protected void onStop() {
    super.onStop();
    binding.tweetInputAddImage.setOnClickListener(null);
    binding.tweetInputText.removeTextChangedListener(viewModel.textWatcher);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    mediaChooserController.onRequestWriteExternalStoragePermissionResult(this, requestCode);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    mediaChooserController.onMediaChooserResult(this, requestCode, resultCode, data);
  }

  private static final String SS_MEDIA_CHOOSER = "ss_mediaChooser";

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    viewModel.onSaveInstanceState(outState);
    outState.putParcelable(SS_MEDIA_CHOOSER, mediaChooserController);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    if (savedInstanceState == null) {
      return;
    }
    mediaChooserController = savedInstanceState.getParcelable(SS_MEDIA_CHOOSER);
    viewModel.onViewStateRestored(savedInstanceState);
  }

  @Override
  public View getRootView() {
    return binding.getRoot();
  }
}
