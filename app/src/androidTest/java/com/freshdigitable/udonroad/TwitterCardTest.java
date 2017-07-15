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

package com.freshdigitable.udonroad;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Created by akihit on 2017/07/15.
 */
public class TwitterCardTest {

  private MockWebServer server;

  @Before
  public void setup() {
    server = new MockWebServer();
  }

  @After
  public void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  public void cancelObserveFetchBeforeFetchingIsDone() throws Exception {
    server.enqueue(new MockResponse()
        .setBody("<html><head></head><body></body></html>")
        .throttleBody(1, 3, TimeUnit.SECONDS));
    server.start();

    final HttpUrl url = server.url("/hoge/fuga.html");
    final Disposable subscribe = TwitterCard.observeFetch(url.toString())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(card -> {}, err -> {});

    final RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
    assertThat(recordedRequest.getPath(), is(url.encodedPath()));
    subscribe.dispose();
    assertThat(subscribe.isDisposed(), is(true));
  }
}