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
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import twitter4j.URLEntity;

/**
 * TwitterCardFetcher fetches twitter card or open graph metadata from specified url.
 *
 * Created by akihit on 2016/09/08.
 */
public class TwitterCardFetcher {
  private static final String TAG = TwitterCardFetcher.class.getSimpleName();

  public static Observable<TwitterCard> observeFetch(final URLEntity urlEntity) {
    final String expandedURL = urlEntity.getExpandedURL();
    final String displayURL = urlEntity.getDisplayURL();
    return Observable.create(new Observable.OnSubscribe<TwitterCard>() {
      @Override
      public void call(Subscriber<? super TwitterCard> subscriber) {
        try {
          final TwitterCard twitterCard = fetch(expandedURL, displayURL);
          subscriber.onNext(twitterCard);
          subscriber.onCompleted();
        } catch (IOException | XmlPullParserException e) {
          subscriber.onError(e);
        }
      }
    }).subscribeOn(Schedulers.io());
  }

  @Nullable
  public static TwitterCard fetch(String url, String displayUrl) throws IOException, XmlPullParserException {
    final OkHttpClient httpClient = new OkHttpClient.Builder()
        .followRedirects(true)
        .build();
    final Request request = new Request.Builder()
        .url(url)
        .build();
    final Call call = httpClient.newCall(request);

    Response response = null;
    List<Meta> metadata = null;
    try {
      response = call.execute();
      final XmlPullParser xmlPullParser = Xml.newPullParser();
      xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlPullParser.setFeature(Xml.FEATURE_RELAXED, true);
      xmlPullParser.setInput(response.body().charStream());

      int eventType = xmlPullParser.getEventType();
      while(eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType != XmlPullParser.START_TAG) {
//          Log.d(TAG, "fetch> ignore:" + xmlPullParser.getName());
          eventType = xmlPullParser.nextTag();
          continue;
        }
        final String name = xmlPullParser.getName();
        if ("head".equalsIgnoreCase(name)) {
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
    if (metadata == null || metadata.isEmpty()) {
      return null;
    }

    String title = null;
    String imageUrl = null;
    for (Meta m : metadata) { // TODO
      if (!m.isTwitterProperty()) {
        continue;
      }
      if ("twitter:title".equals(m.property)) {
        title = m.content;
      } else if (m.property.startsWith("twitter:image")) {
        imageUrl = m.content;
      }
    }
    if (title == null || imageUrl == null) {
      for (Meta m : metadata) {
        if (!m.isOpenGraphProperty()) {
          continue;
        }
        if (title == null && "og:title".equals(m.property)) {
          title = m.content;
        } else if (imageUrl == null && "og:image".equals(m.property)) {
          imageUrl = m.content;
        }
      }
    }
    return (title != null && imageUrl != null)
        ? createCard(url, displayUrl, title, imageUrl)
        : null;
  }

  @NonNull
  private static TwitterCard createCard(String url, String displayUrl, String title, String imageUrl) {
    TwitterCard twitterCard = new TwitterCard();
    twitterCard.title = title;
    twitterCard.imageUrl = imageUrl;
    twitterCard.url = url;
    twitterCard.displayUrl = displayUrl;
    return twitterCard;
  }

  private static List<Meta> readHead(XmlPullParser xpp) throws XmlPullParserException, IOException {
//    xpp.require(XmlPullParser.START_TAG, null, "head");
    if (xpp.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    if (!"head".equalsIgnoreCase(xpp.getName())) {
      throw new IllegalStateException();
    }
    final List<Meta> metadata = new ArrayList<>();
    int eventType = xpp.nextTag();
    while (eventType != XmlPullParser.END_TAG
        || !"head".equalsIgnoreCase(xpp.getName())) {
      if (xpp.getEventType() != XmlPullParser.START_TAG) {
//        Log.d(TAG, "readHead> ignore:" + xpp.getName());
        eventType = xpp.next();
        continue;
      }
      final String name = xpp.getName();
      if ("meta".equalsIgnoreCase(name)) {
//        Log.d(TAG, "readHead> meta:");
        final Meta meta = readMeta(xpp);
        if (meta != null) {
          metadata.add(meta);
        }
        eventType = xpp.nextTag();
      } else {
//        Log.d(TAG, "readHead> skip:" + xpp.getName());
        eventType = xpp.next();
      }
    }
    Log.d(TAG, "readHead: end");
    return metadata;
  }

  private static Meta readMeta(XmlPullParser xpp) throws IOException, XmlPullParserException {
//    xpp.require(XmlPullParser.START_TAG, null, "meta");
    if (xpp.getEventType() != XmlPullParser.START_TAG) {
      throw new IllegalStateException();
    }
    if (!"meta".equalsIgnoreCase(xpp.getName())) {
      throw new IllegalStateException();
    }
    final String name = xpp.getAttributeValue(null, "name");
    String property = name != null
        ? name
        : xpp.getAttributeValue(null, "property");
    String content = xpp.getAttributeValue(null, "content");
//      xpp.require(XmlPullParser.END_TAG, null, "meta");
    Log.d(TAG, "Meta.create> prop:" + property + ", cont:" + content);
    return (Meta.isProperty(property) && content != null)
        ? new Meta(property, content)
        : null;
  }

  private static class Meta {
    final String property;
    final String content;

    Meta(String property, String content) {
      this.property = property;
      this.content = content;
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

    boolean isTwitterProperty() {
      return isTwitterProperty(property);
    }

    boolean isOpenGraphProperty() {
      return isOpenGraphProperty(property);
    }
  }

  private TwitterCardFetcher() {
    throw new AssertionError();
  }
}
