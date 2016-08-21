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
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.freshdigitable.udonroad.MediaContainer.OnMediaClickListener;
import com.freshdigitable.udonroad.StatusViewBase.OnUserIconClickedListener;
import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;
import com.freshdigitable.udonroad.datastore.StatusCache;

import javax.inject.Inject;

import twitter4j.Status;
import twitter4j.User;
import twitter4j.UserMentionEntity;

public class StatusDetailFragment extends Fragment {
  private static final String TAG = StatusDetailFragment.class.getSimpleName();
  private FragmentStatusDetailBinding binding;
  private Status status;
  @Inject
  StatusCache statusCache;
  @Inject
  TwitterApi twitterApi;
  private TimelineSubscriber<StatusCache> statusCacheSubscriber;
  private ArrayAdapter<DetailMenu> arrayAdapter;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    InjectionUtil.getComponent(this).inject(this);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    binding = FragmentStatusDetailBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    arrayAdapter = new ArrayAdapter<DetailMenu>(getContext(), android.R.layout.simple_list_item_1) {
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        Log.d(TAG, "ArrayAdapter.getView: " + getCount());
        View v = super.getView(position, convertView, parent);
        final DetailMenu item = getItem(position);
        final int strres = item.getMenuText();
        ((TextView) v).setText(strres);
        return v;
      }
    };
    binding.detailMenu.setAdapter(arrayAdapter);
    binding.detailMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DetailMenu menu = (DetailMenu) parent.getItemAtPosition(position);
        menu.action(statusCacheSubscriber, status);
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    long id = (long) getArguments().get("statusId");
    statusCache.open(getContext());
    status = statusCache.getStatus(id);
    final UserMentionEntity[] userMentionEntities = status.getUserMentionEntities();
    for (UserMentionEntity u : userMentionEntities) {
      statusCache.upsertUser(u);
    }
    statusCacheSubscriber = new TimelineSubscriber<>(twitterApi, statusCache,
        new TimelineSubscriber.SnackbarFeedback(binding.getRoot()));

    arrayAdapter.add(status.isRetweetedByMe() ? DetailMenu.RT_DELETE : DetailMenu.RT_CREATE);
    arrayAdapter.add(status.isFavorited() ? DetailMenu.FAV_DELETE : DetailMenu.FAV_CREATE);
    arrayAdapter.add(DetailMenu.REPLY);
    arrayAdapter.add(DetailMenu.QUOTE);

    final DetailStatusView statusView = binding.statusView;
    statusView.bindStatus(status);
    StatusViewImageHelper.load(status, statusView);
    final User user = StatusViewImageHelper.getBindingUser(status);
    final ImageView icon = statusView.getIcon();
    icon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onClicked(view, user);
      }
    });
    statusView.getUserName().setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        userIconClickedListener.onClicked(icon, user);
      }
    });
    statusView.getMediaContainer().setOnMediaClickListener(new OnMediaClickListener() {
      @Override
      public void onMediaClicked(View view, int index) {
        MediaViewActivity.start(view.getContext(), status, index);
      }
    });
  }

  @Override
  public void onStop() {
    StatusViewImageHelper.unload(getContext(), status.getId());
    for (int i = arrayAdapter.getCount() - 1; i >= 0; i--) {
      final DetailMenu item = arrayAdapter.getItem(i);
      arrayAdapter.remove(item);
    }
    statusCache.close();
    status = null;
    super.onStop();
  }

  @Override
  public void onDestroyView() {
    binding.statusView.getIcon().setOnClickListener(null);
    binding.statusView.getUserName().setOnClickListener(null);
    binding.statusView.getMediaContainer().setOnMediaClickListener(null);
    binding.statusView.reset();
    binding.detailMenu.setOnItemClickListener(null);
    binding.detailMenu.setAdapter(null);
    super.onDestroyView();
  }


  public static StatusDetailFragment getInstance(final long statusId) {
    Bundle args = new Bundle();
    args.putLong("statusId", statusId);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  enum DetailMenu implements DetailMenuInterface {
    RT_CREATE(R.string.detail_rt_create) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        api.retweetStatus(status.getId());
      }
    }, RT_DELETE(R.string.detail_rt_delete) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        api.destroyRetweet(status.getId());
      }
    }, FAV_CREATE(R.string.detail_fav_create) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        api.createFavorite(status.getId());
      }
    }, FAV_DELETE(R.string.detail_fav_delete) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        api.destroyFavorite(status.getId());
      }
    }, REPLY(R.string.detail_reply) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        //TODO
      }
    }, QUOTE(R.string.detail_quote) {
      @Override
      public void action(TimelineSubscriber<StatusCache> api, Status status) {
        //TODO
      }
    },;

    final private int menuText;

    DetailMenu(@StringRes int menu) {
      this.menuText = menu;
    }

    @StringRes
    public int getMenuText() {
      return menuText;
    }
  }

  interface DetailMenuInterface {
    void action(TimelineSubscriber<StatusCache> api, Status status);
  }

  private OnUserIconClickedListener userIconClickedListener;

  public void setOnUserIconClickedListener(OnUserIconClickedListener listener) {
    this.userIconClickedListener = listener;
  }
}
