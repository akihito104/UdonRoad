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
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.freshdigitable.udonroad.datastore.AppSettingStore;
import com.freshdigitable.udonroad.datastore.SortedCache;
import com.freshdigitable.udonroad.datastore.UpdateEvent;
import com.freshdigitable.udonroad.datastore.UpdateSubject;
import com.freshdigitable.udonroad.datastore.UpdateSubjectFactory;
import com.freshdigitable.udonroad.ffab.IndicatableFFAB;
import com.freshdigitable.udonroad.listitem.ItemViewHolder;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.OnUserIconClickedListener;
import com.freshdigitable.udonroad.listitem.QuotedStatusView;
import com.freshdigitable.udonroad.listitem.StatusView;
import com.freshdigitable.udonroad.listitem.StatusViewHolder;
import com.freshdigitable.udonroad.listitem.TwitterListItem;
import com.freshdigitable.udonroad.module.InjectionUtil;
import com.freshdigitable.udonroad.module.twitter.TwitterApi;
import com.freshdigitable.udonroad.subscriber.ListFetchStrategy;
import com.freshdigitable.udonroad.subscriber.ListRequestWorker;
import com.freshdigitable.udonroad.subscriber.UserFeedbackEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.processors.PublishProcessor;
import twitter4j.MediaEntity;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * OAuthActivity is for OAuth authentication with Twitter.
 *
 * Created by akihit on 15/10/22.
 */
public class OAuthActivity extends AppCompatActivity
    implements FabHandleable, SnackbarCapable, OnUserIconClickedListener {
  private static final String TAG = OAuthActivity.class.getName();

  @Inject
  TwitterApi twitterApi;
  @Inject
  AppSettingStore appSettings;
  @Inject
  UpdateSubjectFactory updateSubjectFactory;
  private UpdateSubject updateSubject;
  @Inject
  PublishProcessor<UserFeedbackEvent> userFeedback;
  private IndicatableFFAB ffab;
  private Toolbar toolbar;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_login);
    InjectionUtil.getComponent(this).inject(this);

    ffab = findViewById(R.id.ffab);

    toolbar = findViewById(R.id.oauth_toolbar);
    setSupportActionBar(toolbar);

    final DemoTimelineFragment demoTimelineFragment = new DemoTimelineFragment();
    final Bundle args = TimelineFragment.createArgs(StoreType.DEMO, -1, "");
    demoTimelineFragment.setArguments(args);
    final List<ListItem> items = new ArrayList<>();
    items.addAll(createItems(getApplicationContext()));
    updateSubject = updateSubjectFactory.getInstance("demo");
    demoTimelineFragment.sortedCache = new DemoSortedCache(items, updateSubject);
    demoTimelineFragment.requestWorker = new ListRequestWorker<ListItem>() {
      @Override
      public ListFetchStrategy getFetchStrategy(StoreType type, long idForQuery, String query) {
        return new DemoListFetcher(items);
      }

      @Override
      public IndicatableFFAB.OnIffabItemSelectedListener getOnIffabItemSelectedListener(long selectedId) {
        return item -> {
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
    };
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.oauth_timeline_container, demoTimelineFragment)
        .commit();
  }

  @Override
  protected void onStart() {
    super.onStart();
    ffab.setOnIffabItemSelectedListener(item -> {
      for (IndicatableFFAB.OnIffabItemSelectedListener listener : ffabListener) {
        listener.onItemSelected(item);
      }
    });
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
  protected void onDestroy() {
    super.onDestroy();
    if (updateSubject != null && !updateSubject.hasCompleted()) {
      updateSubject.onComplete();
    }
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

  private RequestToken requestToken;

  private void startAuthorization() {
    ((MainApplication) getApplication()).logout();
    twitterApi.fetchOAuthRequestToken()
        .observeOn(AndroidSchedulers.mainThread())
        .doOnError(err -> Log.e(TAG, "authentication error: ", err))
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
        .doOnError(err -> Log.e(TAG, "authentication error: ", err))
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
  public void showFab(int type) {
    if (type == TYPE_FAB) {
      ffab.transToFAB();
    } else if (type == TYPE_TOOLBAR) {
      ffab.transToToolbar();
    } else {
      ffab.show();
    }
  }

  @Override
  public void hideFab() {
    ffab.hide();
  }

  @Override
  public void setCheckedFabMenuItem(int itemId, boolean checked) {}

  private List<IndicatableFFAB.OnIffabItemSelectedListener> ffabListener = new ArrayList<>();

  @Override
  public void addOnItemSelectedListener(IndicatableFFAB.OnIffabItemSelectedListener listener) {
    ffabListener.add(listener);
  }

  @Override
  public void removeOnItemSelectedListener(IndicatableFFAB.OnIffabItemSelectedListener listener) {
    ffabListener.remove(listener);
  }

  @Override
  public View getRootView() {
    return findViewById(R.id.oauth_timeline_container);
  }

  @Override
  public void onUserIconClicked(View view, User user) {
    this.userFeedback.onNext(new UserFeedbackEvent(R.string.msg_oauth_user_icon));
  }

  public static class DemoTimelineFragment extends TimelineFragment<ListItem> {
    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      tlAdapter = new DemoTimelineAdapter(sortedCache);
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
    public void onStop() {
      super.onStop();
      ((DemoTimelineAdapter) tlAdapter).loginClickListener = null;
      ((DemoTimelineAdapter) tlAdapter).sendPinClickListener = null;
    }
  }

  private static class DemoListFetcher implements ListFetchStrategy {
    final List<ListItem> items;

    DemoListFetcher(List<ListItem> items) {
      this.items = items;
    }

    @Override
    public void fetch() {}

    @Override
    public void fetchNext() {}
  }


  private static class DemoSortedCache implements SortedCache<ListItem> {
    private final List<ListItem> items;
    private final UpdateSubject updateSubject;

    DemoSortedCache(List<ListItem> items, UpdateSubject updateSubject) {
      this.items = items;
      this.updateSubject = updateSubject;
    }

    @Override
    public Flowable<UpdateEvent> observeUpdateEvent() {
      return updateSubject.observeUpdateEvent();
    }

    @Override
    public long getId(int position) {
      return items.get(position).getId();
    }

    @Override
    public ListItem get(int position) {
      return items.get(position);
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    @Override
    public int getPositionById(long id) {
      return -1;
    }

    @NonNull
    @Override
    public Observable<? extends ListItem> observeById(long id) {
      return Observable.empty();
    }

    @NonNull
    @Override
    public Observable<? extends ListItem> observeById(ListItem element) {
      return Observable.empty();
    }

    @Override
    public void open(String name) {}

    @Override
    public void clear() {}

    @Override
    public void close() {}

    @Override
    public void drop() {}
  }

  private static class DemoTimelineAdapter extends TimelineAdapter<ListItem> {
    private static final int TYPE_AUTH = 0;
    private static final int TYPE_TWEET = 1;

    private DemoTimelineAdapter(SortedCache<ListItem> timelineStore) {
      super(timelineStore);
    }

    @Override
    public int getItemViewType(int position) {
      return timelineStore.get(position) instanceof TwitterListItem ? TYPE_TWEET : TYPE_AUTH;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      return viewType == TYPE_TWEET ? new StatusViewHolder(parent)
          : new DemoViewHolder(View.inflate(parent.getContext(), R.layout.view_pin_auth, null));
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
      if (holder instanceof StatusViewHolder) {
        holder.bind(super.timelineStore.get(position));
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

    @Override
    ListItem wrapListItem(ListItem item) {
      return item;
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


  private static List<ListItem> createItems(Context context) {
    return Arrays.asList(
        new ListItem() {
          @Override
          public long getId() {
            return 1;
          }

          @Override
          public CharSequence getText() {
            return null;
          }

          @Override
          public User getUser() {
            return null;
          }

          @Override
          public CombinedScreenNameTextView.CombinedName getCombinedName() {
            return null;
          }

          @Override
          public List<Stat> getStats() {
            return null;
          }
        },
        getDemoTweet(10, context.getString(R.string.oauth_demo_tweet),
            getDemoTweet(11, context.getString(R.string.oauth_demo_quoted), null)));
  }

  @NonNull
  private static TwitterListItem getDemoTweet(long id, String text, TwitterListItem quoted) {
    return new TwitterListItem() {
      @Override
      public long getId() {
        return id;
      }

      @Override
      public CharSequence getText() {
        return text;
      }

      @Nullable
      @Override
      public TwitterListItem getQuotedItem() {
        return quoted;
      }

      @Override
      public CombinedScreenNameTextView.CombinedName getCombinedName() {
        return new CombinedScreenNameTextView.CombinedName() {
          @Override
          public String getName() {
            return "アオエリヤケイ";
          }

          @Override
          public String getScreenName() {
            return "aoeliyakei";
          }

          @Override
          public boolean isPrivate() {
            return false;
          }

          @Override
          public boolean isVerified() {
            return false;
          }
        };
      }

      @Override
      public List<Stat> getStats() {
        return Collections.emptyList();
      }

      @Override
      public boolean isRetweet() {
        return false;
      }

      @Override
      public String getCreatedTime(Context context) {
        return "now";
      }

      @Override
      public String getSource() {
        return "aoeliyakei";
      }

      @Override
      public int getMediaCount() {
        return 0;
      }

      @Override
      public User getRetweetUser() {
        return null;
      }

      @Nullable
      @Override
      public TimeTextStrategy getTimeStrategy() {
        return null;
      }

      @Override
      public MediaEntity[] getMediaEntities() {
        return new MediaEntity[0];
      }

      @Override
      public boolean isPossiblySensitive() {
        return false;
      }


      @Override
      public User getUser() {
        return null;
      }
    };
  }
}

