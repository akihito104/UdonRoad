/*
 * Copyright (c) 2018. Matsuda, Akihit (akihito104)
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

package com.freshdigitable.udonroad.timeline.fetcher;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.freshdigitable.udonroad.CombinedScreenNameTextView;
import com.freshdigitable.udonroad.R;
import com.freshdigitable.udonroad.StoreType;
import com.freshdigitable.udonroad.listitem.ListItem;
import com.freshdigitable.udonroad.listitem.TwitterListItem;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.IntoMap;
import io.reactivex.Single;
import twitter4j.MediaEntity;
import twitter4j.User;

/**
 * Created by akihit on 2018/01/14.
 */
@Module
public class DemoListFetcherModule {
  @Provides
  @IntoMap
  @ListFetcherModuleKey(StoreType.DEMO)
  ListFetcher<ListItem> providesDemoListFetcher(Context context) {
    final List<ListItem> items = createItems(context);
    return new ListFetcher<ListItem>() {
      @Override
      public Single<? extends List<ListItem>> fetchInit(FetchQuery query) {
        return Single.just(items);
      }

      @Override
      public Single<? extends List<ListItem>> fetchNext(FetchQuery query) {
        return null;
      }
    };
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
