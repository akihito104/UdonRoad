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

import android.support.test.espresso.Espresso;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;

import com.freshdigitable.udonroad.util.AssertionUtil;
import com.freshdigitable.udonroad.util.PerformUtil;
import com.freshdigitable.udonroad.util.TwitterResponseMock;
import com.freshdigitable.udonroad.util.UserUtil;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.util.List;

import twitter4j.IDs;
import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.User;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.getSimpleIdlingResource;
import static com.freshdigitable.udonroad.util.IdlingResourceUtil.runWithIdlingResource;
import static com.freshdigitable.udonroad.util.StatusViewAssertion.recyclerViewDescendantsMatches;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofQuotedStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusView;
import static com.freshdigitable.udonroad.util.StatusViewMatcher.ofStatusViewAt;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createResponseList;
import static com.freshdigitable.udonroad.util.TwitterResponseMock.createStatus;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by akihit on 2016/06/15.
 */
@RunWith(Enclosed.class)
public class MainActivityInstTest {

  @RunWith(AndroidJUnit4.class)
  public static class WhenTimelineIsDefault extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      return setupDefaultTimeline();
    }

    @Test
    public void fetchFav_then_favIconAndCountAreDisplayed() throws Exception {
      // setup
      setupCreateFavorite(0, 1);
      // exec.
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      onView(withId(R.id.ffab)).check(matches(isDisplayed()));
      PerformUtil.favo();
      Espresso.pressBack();
      AssertionUtil.checkFavCountAt(0, 1);
      // TODO tint color check
    }

    @Test
    public void fetchRT_then_RtIconAndCountAreDisplayed() throws Exception {
      // setup
      setupRetweetStatus(25000, 1, 0);
      // exec.
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      onView(withId(R.id.ffab)).check(matches(isDisplayed()));
      performRetweet();
      AssertionUtil.checkRTCountAt(0, 1);
      PerformUtil.clickHeadingOnMenu();
      checkHeadingIsEnabled(false);
      AssertionUtil.checkRTCountAt(0, 1);
      // TODO tint color check
    }

    @Test
    public void receiveStatusDeletionNoticeForLatestStatus_then_removedTheTopOfTimeline()
        throws Exception {
      final Status target = findByStatusId(20000);
      final Status top = findByStatusId(19000);
      receiveDeletionNotice(target);

      onView(ofStatusView(target)).check(doesNotExist());
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(top)));
    }

    @Test
    public void receiveStatusDeletionNoticeForRTStatus_then_removedRTStatus()
        throws Exception {
      // setup
      setupRetweetStatus(25000, 1, 0);
      // exec.
      final Status rtTarget = findByStatusId(20000);
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      performRetweet();
      final Status target = TwitterResponseMock.createRtStatus(rtTarget, 25000, false);
      PerformUtil.pullDownTimeline();
      receiveDeletionNotice(target);

      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(rtTarget)));
    }

    @Test
    public void receiveStatusDeletionNoticeForRTingStatus_then_removedOriginalAndRTedStatuses()
        throws Exception {
      // setup
      setupRetweetStatus(25000, 1, 0);
      // exec.
      final Status target = findByStatusId(20000);
      final Status top = findByStatusId(19000);
      PerformUtil.selectItemView(target);
      checkHeadingIsEnabled(true);
      performRetweet();
      final Status targetRt = TwitterResponseMock.createRtStatus(target, 25000, false);
      receiveDeletionNotice(target, targetRt);

      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(top)));
      onView(ofStatusView(target)).check(doesNotExist());
    }

    @Test
    public void receiveStatusDeletionNoticeForFavedStatus_then_removedOriginalStatuses()
        throws Exception {
      // setup
      setupCreateFavorite(0, 1);
      // exec.
      final Status target = findByStatusId(20000);
      final Status top = findByStatusId(19000);
      PerformUtil.selectItemView(target);
      checkHeadingIsEnabled(true);
      PerformUtil.favo();
      receiveDeletionNotice(target);

      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(top)));
      onView(ofStatusView(target)).check(doesNotExist());
    }

    @Test
    public void receive2StatusDeletionNoticeAtSameTime_then_removed2Statuses()
        throws Exception {
      final Status status18000 = findByStatusId(18000);
      final Status status20000 = findByStatusId(20000);
      final Status top = findByStatusId(19000);
      receiveDeletionNotice(status18000, status20000);

      onView(ofStatusView(status20000)).check(doesNotExist());
      onView(ofStatusView(status18000)).check(doesNotExist());
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(top)));
    }

    @Test
    public void receiveStatusDeletionNoticeForSelectedStatus_then_removedSelectedStatus()
        throws Exception {
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      final Status target = findByStatusId(20000);
      final Status top = findByStatusId(19000);
      receiveDeletionNotice(target);

      onView(ofStatusView(target)).check(doesNotExist());
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(top)));
    }

    @Test
    public void performFavAndRetweet_then_receiveFavoritedAndRetweetedStatus() throws Exception {
      // setup
      setupCreateFavorite(0, 3);
      setupRetweetStatus(25000, 1, 3);
      // exec.
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      PerformUtil.fav_retweet();
      // assert
      PerformUtil.clickHeadingOnMenu();
      checkHeadingIsEnabled(false);
      AssertionUtil.checkFavCountAt(0, 3);
      AssertionUtil.checkRTCountAt(0, 1);
      AssertionUtil.checkFavCountAt(1, 3);
      AssertionUtil.checkRTCountAt(1, 1);
    }

    @Test
    public void receiveStatusUntilShowDetailAndBack_then_showSamePositionOfTimeline() throws Exception {
      final Status target = findByStatusId(20000);
      PerformUtil.selectItemView(target);
      checkHeadingIsEnabled(true);
      PerformUtil.showDetail();
      final Status status = createStatus(25000);
      receiveStatuses(false, status);
      Thread.sleep(1000);
      pressBack();
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(target)));
      PerformUtil.clickHeadingOnMenu();
      checkHeadingIsEnabled(false);
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(status)));
      onView(ofStatusViewAt(R.id.timeline, 1)).check(matches(ofStatusView(target)));
    }

    @Test
    public void receive2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder() throws Exception {
      final Status top = findByStatusId(20000);
      onView(ofStatusView(top)).check(recyclerViewDescendantsMatches(R.id.timeline, 0));
      AssertionUtil.checkFavCountDoesNotExist(top);

      final Status received29 = createStatus(29000);
      final Status received27 = createStatus(27000);
      receiveStatuses(received29, received27);
      onView(ofStatusView(top)).check(recyclerViewDescendantsMatches(R.id.timeline, 2));
      onView(ofStatusView(received29)).check(recyclerViewDescendantsMatches(R.id.timeline, 0));
      onView(ofStatusView(received27)).check(recyclerViewDescendantsMatches(R.id.timeline, 1));
    }

    @Test
    public void receiveDelayed2ReverseStatusIdOrderTweetsAtSameTime_and_displayStatusIdOrder()
        throws Exception {
      final Status received20 = findByStatusId(20000);
      onView(ofStatusView(received20)).check(recyclerViewDescendantsMatches(R.id.timeline, 0));

      final Status received290 = createStatus(29000);
      final Status received195 = createStatus(19500);
      receiveStatuses(received290, received195);
      onView(ofStatusView(received20)).check(recyclerViewDescendantsMatches(R.id.timeline, 1));
      onView(ofStatusView(received290)).check(recyclerViewDescendantsMatches(R.id.timeline, 0));
      onView(ofStatusView(received195)).check(recyclerViewDescendantsMatches(R.id.timeline, 2));
    }

    @Test
    public void heading_then_latestTweetAppears() throws Exception {
      PerformUtil.selectItemViewAt(0);
      checkHeadingIsEnabled(true);
      final Status received = createStatus(22000);
      receiveStatuses(createStatus(21000), received);
      PerformUtil.clickHeadingOnMenu();
      checkHeadingIsEnabled(false);
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(received)));
      onView(withId(R.id.ffab)).check(matches(not(isDisplayed())));
    }

    @Test
    public void receiveStatusWhenStatusIsSelected_then_timelineIsNotScrolled() throws Exception {
      final Status target = findByStatusId(20000);
      PerformUtil.selectItemViewAt(0).check(matches(ofStatusView(target)));
      checkHeadingIsEnabled(true);
      final Status received = createStatus(22000);
      receiveStatuses(received);

      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(target)));
      PerformUtil.clickHeadingOnMenu();
      checkHeadingIsEnabled(false);
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(received)));
      onView(ofStatusViewAt(R.id.timeline, 1)).check(matches(ofStatusView(target)));
    }

    private void performRetweet() {
      final RecyclerView recyclerView = getTimelineView();
      final int expectedCount = (recyclerView != null ? recyclerView.getAdapter().getItemCount() : 0) + 1;
      PerformUtil.retweet();
      runWithIdlingResource(getSimpleIdlingResource("receive status", () -> {
        final RecyclerView rv = getTimelineView();
        return rv != null && rv.getAdapter().getItemCount() == expectedCount;
      }), () ->
          onView(withId(R.id.timeline)).check(matches(isDisplayed())));
    }
  }

  @RunWith(AndroidJUnit4.class)
  public static class WhenAlreadyFavedOrRTed extends Base {
    private static final int EXPECTED_FAV_COUNT = 3;
    private static final int EXPECTED_RT_COUNT = 5;
    private final Status faved = createStatus(21000);
    private final Status rted = createStatus(22000);

    @Override
    protected int setupTimeline() throws TwitterException {
      final ResponseList<Status> responseList = createDefaultResponseList(getLoginUser());
      when(faved.getFavoriteCount()).thenReturn(EXPECTED_FAV_COUNT);
      responseList.add(faved);
      when(rted.getRetweetCount()).thenReturn(EXPECTED_RT_COUNT);
      responseList.add(rted);
      this.responseList = responseList;
      when(twitter.getHomeTimeline()).thenReturn(responseList);
      when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());
      return responseList.size();
    }

    @Test
    public void performFavorite_then_receiveTwitterExceptionForAlreadyFavorited() throws Exception {
      final TwitterException twitterException = mock(TwitterException.class);
      when(twitterException.getStatusCode()).thenReturn(403);
      when(twitterException.getErrorCode()).thenReturn(139);
      when(twitter.createFavorite(anyLong())).thenThrow(twitterException);
      // exec.
      PerformUtil.selectItemView(faved);
      checkHeadingIsEnabled(true);
      PerformUtil.favo();
      // assert
//      onView(withText(R.string.msg_already_fav)).check(matches(isDisplayed()));
      AssertionUtil.checkFavCount(faved, EXPECTED_FAV_COUNT);
    }

    @Test
    public void performRetweet_then_receiveTwitterExceptionForAlreadyRetweeted() throws Exception {
      // setup
      final TwitterException twitterException = mock(TwitterException.class);
      when(twitterException.getStatusCode()).thenReturn(403);
      when(twitterException.getErrorCode()).thenReturn(327);
      when(twitter.retweetStatus(anyLong())).thenThrow(twitterException);
      // exec.
      PerformUtil.selectItemView(rted);
      checkHeadingIsEnabled(true);
      PerformUtil.retweet();
      // assert
//      onView(withText(R.string.msg_already_rt)).check(matches(isDisplayed()));
      AssertionUtil.checkRTCount(rted, EXPECTED_RT_COUNT);
    }

    @Test
    public void performFavAndRetweet_then_receiveTwitterExceptionForAlreadyFavorited() throws Exception {
      // setup
      final TwitterException twitterException = mock(TwitterException.class);
      when(twitterException.getStatusCode()).thenReturn(403);
      when(twitterException.getErrorCode()).thenReturn(139);
      when(twitter.createFavorite(anyLong())).thenThrow(twitterException);
      setupRetweetStatus(25000, 1, EXPECTED_FAV_COUNT);
      // exec.
      PerformUtil.selectItemView(faved);
      checkHeadingIsEnabled(true);
      PerformUtil.fav_retweet();
      // assert
      PerformUtil.clickHeadingOnMenu();
      AssertionUtil.checkRTCountAt(0, 1);
      AssertionUtil.checkFavCountAt(0, EXPECTED_FAV_COUNT);
      AssertionUtil.checkRTCount(faved, 1);
      AssertionUtil.checkFavCount(faved, EXPECTED_FAV_COUNT);
    }
  }

  public static class WhenHomeTimelineDelayed extends Base {
    @Override
    protected int setupTimeline() throws TwitterException {
      final List<Status> statuses = createDefaultStatuses(getLoginUser());
      for (int i = 0; i < statuses.size(); i++) {
        final Status s = statuses.get(i);
        final long quotedId = s.getId() - 1;
        final Status quoted = createStatus(quotedId, s.getUser());
        when(s.getQuotedStatus()).thenReturn(quoted);
        when(s.getQuotedStatusId()).thenReturn(quotedId);
      }
      final ResponseList<Status> responseList = createResponseList(statuses);
      this.responseList = responseList;
      when(twitter.getHomeTimeline()).thenAnswer(invocation -> {
        Thread.sleep(2000L);
        return responseList;
      });
      final long maxId = getMaxId(statuses);
      when(twitter.getHomeTimeline(argThat(p -> p.getMaxId() > maxId))).thenReturn(responseList);
      return 0;
    }

    private static long getMaxId(List<Status> statuses) {
      long maxId = -1;
      for (Status s : statuses) {
        if (maxId < s.getId()) {
          maxId = s.getId();
        }
      }
      return maxId;
    }

    @Test
    public void showOneQuotedTweetViewInEachTweetView() throws Exception {
      final Status target = createStatus(25000);
      receiveStatuses(21, target);
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(target)));

      verify(twitter, times(1)).getHomeTimeline(any(Paging.class));
      onView(ofQuotedStatusView(createStatus(19999))).check(matches(isDisplayed()));
    }
  }

  public static class WhenFetchingIgnoringUsersIsFailed extends Base {
    @Test
    public void attachTimeline() {
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
    }

    @Override
    protected int setupTimeline() throws TwitterException {
      return setupDefaultTimeline();
    }

    @Override
    void setupIgnoringUsers() throws TwitterException {
      when(twitter.getBlocksIDs(anyLong())).thenThrow(new TwitterException(""));
      when(twitter.getMutesIDs(anyLong())).thenThrow(new TwitterException(""));
    }
  }

  public static class WhenFetchingIgnoringUsersIsDelayed extends Base {
    private static final long IGNORED_USER_ID = 30001;
    private final User ignored = UserUtil.builder(IGNORED_USER_ID, "ignored").build();
    private final Status ignoredTarget = createStatus(20001, ignored);
    private boolean receivedBlocks = false;
    private boolean receivedMutes = false;
    private boolean blocked = true;

    @Test
    public void removeStatusAfterTimelineIsAttached() {
      onView(withId(R.id.action_heading)).check(matches(isDisplayed()));
      onView(ofStatusViewAt(R.id.timeline, 0)).check(matches(ofStatusView(ignoredTarget)));
      blocked = false;
      runWithIdlingResource(getSimpleIdlingResource("remove ignored",
          () -> receivedBlocks && receivedMutes),
          () -> onView(ofStatusView(ignoredTarget)).check(doesNotExist()));
    }

    @Override
    protected int setupTimeline() throws TwitterException {
      final ResponseList<Status> responseList = createDefaultResponseList(getLoginUser());
      responseList.add(ignoredTarget);
      this.responseList = responseList;
      when(twitter.getHomeTimeline()).thenReturn(responseList);
      when(twitter.getHomeTimeline(any(Paging.class))).thenReturn(createResponseList());
      return responseList.size();
    }

    @Override
    void setupIgnoringUsers() throws TwitterException {
      final IDs ignoringUserIDsMock = mock(IDs.class);
      when(ignoringUserIDsMock.getIDs()).thenReturn(new long[]{IGNORED_USER_ID});
      when(ignoringUserIDsMock.getNextCursor()).thenReturn(0L);
      when(ignoringUserIDsMock.getPreviousCursor()).thenReturn(0L);
      when(ignoringUserIDsMock.hasNext()).thenReturn(false);
      when(twitter.getBlocksIDs(anyLong())).thenAnswer(invocation -> {
        while(blocked) {
          Thread.sleep(500L);
        }
        receivedBlocks = true;
        return ignoringUserIDsMock;
      });
      when(twitter.getMutesIDs(anyLong())).thenAnswer(invocation -> {
        while(blocked) {
          Thread.sleep(500L);
        }
        receivedMutes = true;
        return ignoringUserIDsMock;
      });
    }

    @Override
    public void tearDown() throws Exception {
      receivedBlocks = false;
      receivedMutes = false;
      super.tearDown();
    }
  }

  private static abstract class Base extends TimelineInstTestBase {
    @Rule
    public final ActivityTestRule<MainActivity> rule
        = new ActivityTestRule<>(MainActivity.class, false, false);

    @Override
    protected ActivityTestRule<MainActivity> getRule() {
      return rule;
    }
  }

  private static void checkHeadingIsEnabled(boolean enabled) {
    onView(withId(R.id.action_heading)).check(matches(enabled ? isEnabled() : not(isEnabled())));
  }
}