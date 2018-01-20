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

import android.app.Activity;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnItemViewClickListener;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.listitem.StatusViewImageLoader;
import com.freshdigitable.udonroad.listitem.TwitterListItem;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;
import com.freshdigitable.udonroad.timeline.repository.ListItemRepository;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.PublishProcessor;
import timber.log.Timber;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import static com.freshdigitable.udonroad.FabViewModel.Type.FAB;
import static com.freshdigitable.udonroad.FabViewModel.Type.HIDE;
import static com.freshdigitable.udonroad.FabViewModel.Type.TOOLBAR;

/**
 * OAuthActivity is for OAuth authentication with Twitter.
 *
 * Created by akihit on 15/10/22.
 */
public class OAuthActivity extends AppCompatActivity
    implements SnackbarCapable, OnUserIconClickedListener {
  private static final String TAG = OAuthActivity.class.getName();

  @Inject
  TwitterApi twitterApi;
  @Inject
  AppSettingStore appSettings;
  @Inject
  PublishProcessor<UserFeedbackEvent> userFeedback;
  private IndicatableFFAB ffab;
  private Toolbar toolbar;
  private FabViewModel fabViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    InjectionUtil.getComponent(this).inject(this);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    ffab = findViewById(R.id.ffab);
    toolbar = findViewById(R.id.oauth_toolbar);
    setSupportActionBar(toolbar);

    if (savedInstanceState == null) {
      final DemoTimelineFragment demoTimelineFragment = new DemoTimelineFragment();
      final Bundle args = TimelineFragment.createArgs(StoreType.DEMO, -1, "");
      demoTimelineFragment.setArguments(args);
      getSupportFragmentManager().beginTransaction()
          .replace(R.id.oauth_timeline_container, demoTimelineFragment)
          .commit();
    }
    fabViewModel = ViewModelProviders.of(this).get(FabViewModel.class);
    fabViewModel.getFabState().observe(this, type -> {
      if (type == FAB) {
        ffab.transToFAB();
      } else if (type == TOOLBAR) {
        ffab.transToToolbar();
      } else if (type == HIDE) {
        ffab.hide();
      } else {
        ffab.show();
      }
    });
  }

  @Override
  protected void onStart() {
    super.onStart();
    ffab.setOnIffabItemSelectedListener(fabViewModel::onMenuItemSelected);
    appSettings.open();
    final List<? extends User> users = appSettings.getAllAuthenticatedUsers();
    if (!users.isEmpty()) {
      toolbar.setTitle(R.string.title_add_account);
    }
    appSettings.close();
  }

  @Override
  protected void onStop() {
    super.onStop();
    ffab.setOnIffabItemSelectedListener(null);
  }

  @Override
  public void onBackPressed() {
    appSettings.open();
    final List<? extends User> users = appSettings.getAllAuthenticatedUsers();
    if (!users.isEmpty()) {
      startActivity(new Intent(this, getRedirect()));
    }
    appSettings.close();
    super.onBackPressed();
  }

  private static final String SS_REQUEST_TOKEN = "ss_requestToken";
  private RequestToken requestToken;

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putSerializable(SS_REQUEST_TOKEN, requestToken);
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);
    requestToken = (RequestToken) savedInstanceState.getSerializable(SS_REQUEST_TOKEN);
  }

  private void startAuthorization() {
    ((MainApplication) getApplication()).logout();
    twitterApi.fetchOAuthRequestToken()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(err -> Timber.tag(TAG).e(err, "authentication error: "))
        .subscribe(this::startAuthAction,
            e -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_failed)));
  }

  private void startAuthAction(RequestToken token) {
    requestToken = token;
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(token.getAuthorizationURL()));
    startActivity(intent);
  }

  private void startAuthentication(final String verifier) {
    twitterApi.fetchOAuthAccessToken(requestToken, verifier)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(err -> Timber.tag(TAG).e(err, "authentication error: "))
        .subscribe(this::checkOAuth,
            e -> userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_failed)));
  }

  private void checkOAuth(AccessToken accessToken) {
    userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_success));
    appSettings.open();
    appSettings.storeAccessToken(accessToken);
    ((MainApplication) getApplication()).logout();
    appSettings.close();
    ((MainApplication) getApplication()).login(accessToken.getUserId());
    Intent intent = new Intent(this, getRedirect());
    startActivity(intent);
    finish();
  }

  private static final String EXTRAS_REDIRECT = "redirect";

  public static Intent createIntent(Activity redirect) {
    if (redirect instanceof OAuthActivity) {
      throw new IllegalArgumentException();
    }
    return createIntent(redirect.getApplicationContext(), redirect.getClass());
  }

  public static Intent createIntent(Context context, Class<? extends Activity> clz) {
    final Intent intent = new Intent(context, OAuthActivity.class);
    intent.putExtra(EXTRAS_REDIRECT, clz);
    return intent;
  }

  private Class<?> getRedirect() {
    final Bundle extras = getIntent().getExtras();
    return extras != null ? (Class<?>) extras.getSerializable(EXTRAS_REDIRECT)
        : MainActivity.class;
  }

  public static void start(Activity redirect) {
    final Intent intent = createIntent(redirect);
    redirect.startActivity(intent);
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.oauth_timeline_container);
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    this.userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_user_icon));
  }

  public static class DemoTimelineFragment extends TimelineFragment {
    @Inject
    PublishProcessor<UserFeedbackEvent> userFeedback;

    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      InjectionUtil.getComponent(this).inject(this);
    }

    @Override
    public void onStart() {
      super.onStart();
      ((DemoTimelineAdapter) tlAdapter).loginClickListener = v ->
          ((OAuthActivity) getActivity()).startAuthorization();
      ((DemoTimelineAdapter) tlAdapter).sendPinClickListener = (v, pin) ->
          ((OAuthActivity) getActivity()).startAuthentication(pin);
    }

    @Override
    Observer<MenuItem> getMenuItemObserver() {
      return item -> {
        if (item == null) {
          return;
        }
        final int itemId = item.getItemId();
        if (itemId == R.id.iffabMenu_main_fav) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_fav));
        } else if (itemId == R.id.iffabMenu_main_rt) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_rt));
        } else if (itemId == R.id.iffabMenu_main_favRt) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_favRt));
        } else if (itemId == R.id.iffabMenu_main_detail) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_detail));
        } else if (itemId == R.id.iffabMenu_main_conv) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_conv));
        } else if (itemId == R.id.iffabMenu_main_reply) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_reply));
        } else if (itemId == R.id.iffabMenu_main_quote) {
          userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_quote));
        }
      };
    }

    @Override
    public void onStop() {
      super.onStop();
      ((DemoTimelineAdapter) tlAdapter).loginClickListener = null;
      ((DemoTimelineAdapter) tlAdapter).sendPinClickListener = null;
    }

    @Override
    TimelineAdapter createTimelineAdapter() {
      return new DemoTimelineAdapter(repository, imageLoader);
    }

    @Override
    OnItemViewClickListener createOnItemViewClickListener() {
      return null;
    }
  }

  private static class DemoTimelineAdapter extends TimelineAdapter {
    private static final int TYPE_AUTH = 0;
    private static final int TYPE_TWEET = 1;

    private DemoTimelineAdapter(ListItemRepository timelineStore, StatusViewImageLoader imageLoader) {
      super(timelineStore, imageLoader);
    }

    @Override
    public int getItemViewType(int position) {
      return repository.get(position) instanceof TwitterListItem ? TYPE_TWEET : TYPE_AUTH;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return viewType == TYPE_TWEET ? new StatusViewHolder(parent)
          : new DemoViewHolder(View.inflate(parent.getContext(), R.layout.view_pin_auth, null));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
      if (holder instanceof StatusViewHolder) {
        holder.bind(super.repository.get(position), imageLoader);
        final ImageView userIcon = holder.getUserIcon();
        final Drawable icon = AppCompatResources.getDrawable(userIcon.getContext(), R.mipmap.ic_launcher);
        userIcon.setImageDrawable(icon);
        final QuotedStatusView quotedStatusView = ((StatusView) holder.itemView).getQuotedStatusView();
        if (quotedStatusView != null) {
          quotedStatusView.getIcon().setImageDrawable(icon);
        }
      }
    }

    interface OnSendPinClickListener {
      void onClick(View v, String pin);
    }

    private View.OnClickListener loginClickListener;
    private OnSendPinClickListener sendPinClickListener;

    @Override
    public void onViewAttachedToWindow(ItemViewHolder holder) {
      if (holder instanceof DemoViewHolder) {
        ((DemoViewHolder) holder).oauthButton.setOnClickListener(loginClickListener);
        ((DemoViewHolder) holder).sendPin.setOnClickListener(v ->
            sendPinClickListener.onClick(v, ((DemoViewHolder) holder).pin.getText().toString()));
      } else {
        super.onViewAttachedToWindow(holder);
      }
    }

    @Override
    public void onViewDetachedFromWindow(ItemViewHolder holder) {
      if (holder instanceof DemoViewHolder) {
        ((DemoViewHolder) holder).oauthButton.setOnClickListener(null);
        ((DemoViewHolder) holder).sendPin.setOnClickListener(null);
      } else {
        super.onViewDetachedFromWindow(holder);
      }
    }
  }

  private static class DemoViewHolder extends ItemViewHolder {
    private View oauthButton;
    private EditText pin;
    private Button sendPin;

    DemoViewHolder(View view) {
      super(view);
      oauthButton = view.findViewById(R.id.oauth_start);
      pin = view.findViewById(R.id.oauth_pin);
      sendPin = view.findViewById(R.id.oauth_send_pin);
    }

    @Override
    public ImageView getUserIcon() {
      return null;
    }

    @Override
    public void onUpdate(ListItem item) {}

    @Override
    public void onSelected(long itemId) {}

    @Override
    public void onUnselected(long itemId) {}
  }
}
