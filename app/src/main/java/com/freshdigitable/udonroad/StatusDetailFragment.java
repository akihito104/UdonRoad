/*
 * Copyright (c) 2016. UdonRoad by Akihito Matsuda (akihito104)
 */

package com.freshdigitable.udonroad;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.freshdigitable.udonroad.databinding.FragmentStatusDetailBinding;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import twitter4j.Status;

public class StatusDetailFragment extends Fragment {
  private FragmentStatusDetailBinding binding;
  private Status status;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return  inflater.inflate(R.layout.fragment_status_detail, container, false);
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    binding = DataBindingUtil.bind(getView());

    this.status = (Status) getArguments().get("status");
    binding.statusView.bindStatus(this.status);

    final ArrayAdapter<DetailMenu> arrayAdapter
        = new ArrayAdapter<DetailMenu>(getContext(), android.R.layout.simple_list_item_1){
      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);
        final DetailMenu item = getItem(position);
        final int strres = item.getStringRes();
        ((TextView)v).setText(strres);
        return v;
      }
    };

    binding.detailMenu.setAdapter(arrayAdapter);
    arrayAdapter.add(status.isRetweetedByMe() ? DetailMenu.RT_DELETE : DetailMenu.RT_CREATE);
    arrayAdapter.add(status.isFavorited() ? DetailMenu.FAV_DELETE : DetailMenu.FAV_CREATE);
    arrayAdapter.add(DetailMenu.REPLY);
    arrayAdapter.add(DetailMenu.QUOTE);
    binding.detailMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final DetailMenu menu = (DetailMenu) parent.getItemAtPosition(position);
        menu.action(twitterApi, status, getContext());
      }
    });
  }

  private TwitterApi twitterApi;

  public void setTwitterApi(TwitterApi twitterApi) {
    this.twitterApi = twitterApi;
  }

  public static StatusDetailFragment getInstance(final Status status) {
    Bundle args = new Bundle();
    args.putSerializable("status", status);
    final StatusDetailFragment statusDetailFragment = new StatusDetailFragment();
    statusDetailFragment.setArguments(args);
    return statusDetailFragment;
  }

  enum DetailMenu implements DetailMenuInterface {
    RT_CREATE {
      @Override
      public int getStringRes() {
        return R.string.detail_rt_create;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.retweetStatus(status.getId()), context,
            R.string.detail_rt_create_failed, R.string.detail_rt_create_success);
      }
    }, RT_DELETE {
      @Override
      public int getStringRes() {
        return R.string.detail_rt_delete;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.destroyStatus(status.getId()), context,
            R.string.detail_rt_delete_failed, R.string.detail_rt_delete_success);

      }
    }, FAV_CREATE {
      @Override
      public int getStringRes() {
        return R.string.detail_fav_create;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.createFavorite(status.getId()), context,
            R.string.detail_fav_create_failed, R.string.detail_fav_create_success);
      }
    }, FAV_DELETE {
      @Override
      public int getStringRes() {
        return R.string.detail_fav_delete;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        doAction(api.destroyFavorite(status.getId()), context,
            R.string.detail_fav_delete_failed, R.string.detail_fav_delete_success);
      }
    }, REPLY {
      @Override
      public int getStringRes() {
        return R.string.detail_reply;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        //TODO
      }
    }, QUOTE {
      @Override
      public int getStringRes() {
        return R.string.detail_quote;
      }

      @Override
      public void action(TwitterApi api, Status status, Context context) {
        //TODO
      }
    },;

    void doAction(Observable<Status> observable, Context context,
                  @StringRes int onErrorMessage, @StringRes int onCompleteMessage) {
      observable.observeOn(AndroidSchedulers.mainThread())
          .subscribe(onNext(),
              onErrorToast(context, onErrorMessage),
              onCompleteToast(context, onCompleteMessage));
    }

    private Action0 onCompleteToast(final Context context, final @StringRes int res) {
      return new Action0() {
        @Override
        public void call() {
          Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
        }
      };
    }

    private Action1<Throwable> onErrorToast(final Context context, final @StringRes int res) {
      return new Action1<Throwable>() {
        @Override
        public void call(Throwable o) {
          Toast.makeText(context, res, Toast.LENGTH_SHORT).show();
        }
      };
    }

    private Action1<Status> onNext() {
      return new Action1<Status>() {
        @Override
        public void call(Status o) {
        }
      };
    }
  }

  interface DetailMenuInterface {
    @StringRes
    int getStringRes();

    void action(TwitterApi api, Status status, Context context);
  }
}
