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

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;

/**
 * TwitterCardFetcher fetches twitter card or open graph metadata from specified url.
 *
 * Created by akihit on 2016/09/08.
 */
public class TwitterCardFetcher {
  private static final String TAG = TwitterCardFetcher.class.getSimpleName();

  public static Observable<TwitterCard> observeFetch(final String expandedURL) {
    return Observable.create(new Observable.OnSubscribe<TwitterCard>() {
      @Override
      public void call(Subscriber<? super TwitterCard> subscriber) {
        try {
          final TwitterCard twitterCard = fetch(expandedURL);
          subscriber.onNext(twitterCard);
          subscriber.onCompleted();
        } catch (IOException | XmlPullParserException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  private static TwitterCard fetch(String url) throws IOException, XmlPullParserException {
    final OkHttpClient httpClient = new OkHttpClient.Builder()
        .followRedirects(true)
        .build();
    final Request request = new Request.Builder()
        .url(url)
        .build();
    final Call call = httpClient.newCall(request);

    Response response = null;
    List<Meta> metadata = null;
    final String expandedUrl;
    try {
      response = call.execute();
      expandedUrl = response.request().url().toString();
      final XmlPullParser xmlPullParser = Xml.newPullParser();
      xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlPullParser.setFeature(Xml.FEATURE_RELAXED, true);
      xmlPullParser.setInput(response.body().charStream());

      int eventType = xmlPullParser.getEventType();
      while(eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType != XmlPullParser.START_TAG) {
          eventType = xmlPullParser.nextTag();
          continue;
        }
        final String name = xmlPullParser.getName();
        if (isHeadTag(name)) {
          Log.d(TAG, "fetch> head:");
          metadata = readHead(xmlPullParser);
          break;
        }
        eventType = xmlPullParser.next();
      }
    } finally {
      if (response != null) {
        response.close();
      }
    }

    final TwitterCard card = createCard(metadata, expandedUrl);
    card.setTweetedUrl(url);
    return card;
  }

  @NonNull
  private static TwitterCard createCard(List<Meta> metadata, String url) {
    final TwitterCard twitterCard = new TwitterCard();
    twitterCard.setUrl(url);

    if (metadata == null || metadata.isEmpty()) {
      return twitterCard;
    }

    Map<String, String> maps = new HashMap<>();
    for (Meta m : metadata) {
      maps.put(m.property, m.content);
    }
    twitterCard.setTitle(getTitle(maps));
    twitterCard.setImageUrl(getImageUrl(maps));
    twitterCard.setAppUrl(maps.get("twitter:app:url:googleplay"));
    return twitterCard;
  }

  private static String getTitle(Map<String, String> maps) {
    return maps.containsKey("twitter:title")
        ? maps.get("twitter:title") : maps.get("og:title");
  }

  private static String getImageUrl(Map<String, String> maps) {
    for (String key : maps.keySet()) {
      if (key.startsWith("twitter:image")) {
        return maps.get(key);
      }
    }
    for (String key : maps.keySet()) {
      if (key.equals("og:image")) {
        return maps.get(key);
      }
    }
    return "";
  }

  private static List<Meta> readHead(XmlPullParser xpp) throws XmlPullParserException, IOException {
    if (xpp.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    if (!isHeadTag(xpp.getName())) {
      throw new IllegalStateException();
    }
    final List<Meta> metadata = new ArrayList<>();
    int eventType = xpp.nextTag();
    while (eventType != XmlPullParser.END_TAG
        || !isHeadTag(xpp.getName())) {
      if (xpp.getEventType() != XmlPullParser.START_TAG) {
        eventType = xpp.next();
        continue;
      }
      if (Meta.isMetaTag(xpp.getName())) {
        final Meta meta = readMeta(xpp);
        if (meta != null) {
          metadata.add(meta);
        }
        eventType = xpp.nextTag();
      } else {
        eventType = xpp.next();
      }
    }
    Log.d(TAG, "readHead: end");
    return metadata;
  }

  private static Meta readMeta(XmlPullParser xpp) throws IOException, XmlPullParserException {
    if (xpp.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    if (!Meta.isMetaTag(xpp.getName())) {
      throw new IllegalStateException();
    }
    final String name = xpp.getAttributeValue(null, "name");
    String property = Meta.isTwitterProperty(name)
        ? name
        : xpp.getAttributeValue(null, "property");
    String content = xpp.getAttributeValue(null, "content");
    Log.d(TAG, "Meta.create> prop:" + property + ", cont:" + content);
    return (Meta.isProperty(property) && content != null)
        ? new Meta(property, content)
        : null;
  }

  private static boolean isHeadTag(String tag) {
    return "head".equalsIgnoreCase(tag);
  }

  private static class Meta {
    final String property;
    final String content;

    Meta(String property, String content) {
      this.property = property;
      this.content = content;
    }

    static boolean isMetaTag(String tag) {
      return "meta".equalsIgnoreCase(tag);
    }

    static boolean isTwitterProperty(String property) {
      return property != null && property.startsWith("twitter:");
    }

    static boolean isOpenGraphProperty(String property) {
      return property != null && property.startsWith("og:");
    }

    static boolean isProperty(String property) {
      return isTwitterProperty(property) || isOpenGraphProperty(property);
    }
  }

  private TwitterCardFetcher() {
    throw new AssertionError();
  }
}
