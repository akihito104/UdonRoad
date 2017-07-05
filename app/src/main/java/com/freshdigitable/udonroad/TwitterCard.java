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

import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * TwitterCard defines data to create TwitterCardView.
 *
 * Created by akihit on 2016/09/08.
 */
public class TwitterCard {
  private final String url;
  private final Map<Property, String> properties;

  public TwitterCard() {
    this("");
  }

  private TwitterCard(String url) {
    this(url, Collections.emptyMap());
  }

  private TwitterCard(String url, Map<Property, String> properties) {
    this.url = url;
    this.properties = properties;
  }

  public String getUrl() {
    return url;
  }

  public String getImageUrl() {
    return properties.containsKey(Property.TWITTER_IMAGE)
        ? properties.get(Property.TWITTER_IMAGE) : properties.get(Property.OG_IMAGE);
  }

  public String getTitle() {
    return properties.containsKey(Property.TWITTER_TITLE)
        ? properties.get(Property.TWITTER_TITLE) : properties.get(Property.OG_TITLE);
  }

  public String getDisplayUrl() {
    return Uri.parse(url).getHost();
  }

  public String getAppUrl() {
    return properties.get(Property.TWITTER_APP_URL_GOOGLEPLAY);
  }

  public boolean isValid() {
    return !TextUtils.isEmpty(getTitle()) && !TextUtils.isEmpty(getUrl());
  }

  @NonNull
  public static Observable<TwitterCard> observeFetch(final String expandedURL) {
    final Call call = Fetcher.createCall(expandedURL);
    return Observable.create((ObservableOnSubscribe<TwitterCard>) subscriber -> {
      try {
        final TwitterCard twitterCard = Fetcher.fetch(call);
        subscriber.onNext(twitterCard);
        subscriber.onComplete();
      } catch (IOException | XmlPullParserException e) {
        subscriber.onError(e);
      }
    }).subscribeOn(Schedulers.io())
        .doOnDispose(call::cancel);
  }

  private static class Fetcher {
    private static final String TAG = Fetcher.class.getSimpleName();
    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
        .followRedirects(true)
        .build();

    private static Call createCall(String url) {
      final Request request = new Request.Builder()
          .url(url)
          .build();
      return httpClient.newCall(request);
    }

    @NonNull
    private static TwitterCard fetch(Call call) throws IOException, XmlPullParserException {
      Response response = null;
      try {
        response = call.execute();
        final ResponseBody body = response.body();
        if (body != null) {
          final Map<Property, String> metadata = findMetaTagForCard(body.charStream());
          return new TwitterCard(response.request().url().toString(), metadata);
        }
        throw new IllegalStateException("twitter card fetch error...");
      } finally {
        if (response != null) {
          Log.d(TAG, "fetch: close response");
          response.close();
        }
      }
    }

    @NonNull
    private static Map<Property, String> findMetaTagForCard(Reader reader)
        throws XmlPullParserException, IOException {
      Log.d(TAG, "findMetaTagForCard: ");
      final XmlPullParser xmlPullParser = Xml.newPullParser();
      xmlPullParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      xmlPullParser.setFeature(Xml.FEATURE_RELAXED, true);
      xmlPullParser.setInput(reader);

      int eventType = xmlPullParser.getEventType();
      while(eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType != XmlPullParser.START_TAG) {
          eventType = xmlPullParser.nextTag();
          continue;
        }
        final String name = xmlPullParser.getName();
        if (isHeadTag(name)) {
          Log.d(TAG, "fetch> head:");
          return readHead(xmlPullParser);
        }
        eventType = xmlPullParser.next();
      }
      return Collections.emptyMap();
    }

    @NonNull
    private static Map<Property, String> readHead(XmlPullParser xpp)
        throws XmlPullParserException, IOException {
      if (xpp.getEventType() != XmlPullParser.START_TAG) {
        throw new IllegalStateException();
      }
      if (!isHeadTag(xpp.getName())) {
        throw new IllegalStateException();
      }
      final Map<Property, String> metadata = new HashMap<>();
      int eventType = xpp.nextTag();
      while (eventType != XmlPullParser.END_TAG || !isHeadTag(xpp.getName())) {
        if (xpp.getEventType() != XmlPullParser.START_TAG) {
          eventType = xpp.next();
          continue;
        }
        if (isMetaTag(xpp.getName())) {
          final Property property = readMetaProperty(xpp);
          if (property != Property.UNKNOWN) {
            final String content = readContent(xpp);
            metadata.put(property, content);
          }
          eventType = xpp.nextTag();
        } else {
          eventType = xpp.next();
        }
      }
      Log.d(TAG, "readHead: end");
      return metadata;
    }

    @NonNull
    private static Property readMetaProperty(XmlPullParser xpp)
        throws IOException, XmlPullParserException {
      if (xpp.getEventType() != XmlPullParser.START_TAG) {
        throw new IllegalStateException();
      }
      if (!isMetaTag(xpp.getName())) {
        throw new IllegalStateException();
      }
      final Property p = Property.findByString(xpp.getAttributeValue(null, "name"));
      return p != Property.UNKNOWN
          ? p : Property.findByString(xpp.getAttributeValue(null, "property"));
    }

    private static String readContent(XmlPullParser xpp) {
      return xpp.getAttributeValue(null, "content");
    }

    private static boolean isHeadTag(String tag) {
      return "head".equalsIgnoreCase(tag);
    }

    private static boolean isMetaTag(String tag) {
      return "meta".equalsIgnoreCase(tag);
    }

    private Fetcher() {
      throw new AssertionError();
    }
  }

  private enum Property {
    TWITTER_TITLE, TWITTER_IMAGE, OG_TITLE, OG_IMAGE, TWITTER_APP_URL_GOOGLEPLAY, UNKNOWN;

    private String toAttrString() {
      return name().toLowerCase(Locale.ROOT).replaceAll("_", ":");
    }

    static Property findByString(String property) {
      for (Property p : values()) {
        if (p.toAttrString().equals(property)) {
          return p;
        }
      }
      return UNKNOWN;
    }
  }
}
