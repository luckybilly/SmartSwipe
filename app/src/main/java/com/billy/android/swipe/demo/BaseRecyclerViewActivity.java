package com.billy.android.swipe.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.billy.android.swipe.SmartSwipeRefresh;
import com.billy.android.swipe.demo.consumer.ImageViewerActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * base activity
 * @author billy.qi
 */
public abstract class BaseRecyclerViewActivity extends BaseActivity {

    protected RecyclerView recyclerView;
    protected RecyclerAdapter adapter;

    String[] names;
    String[] messages;
    private AtomicInteger idMaker = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_recycler_view);
        recyclerView = findViewById(R.id.recyclerView);


        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        adapter = new RecyclerAdapter(getInitData());
        recyclerView.setAdapter(adapter);
    }

    protected SmartSwipeRefresh.SmartSwipeRefreshDataLoader dataLoader = new SmartSwipeRefresh.SmartSwipeRefreshDataLoader() {
        @Override
        public void onRefresh(final SmartSwipeRefresh ssr) {
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    adapter.refreshData(getData());
                    ssr.finished(true);
                }
            }, 2000);
        }

        @Override
        public void onLoadMore(final SmartSwipeRefresh ssr) {
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    adapter.addData(getData());
                    ssr.setNoMoreData(true);
                    ssr.finished(true);
                }
            }, 500);
        }
    };

    protected List<Data> getInitData() {
        return getData();
    }

    protected List<Data> getData() {
        if (names == null) {
            names = getResources().getStringArray(R.array.demo_names);
            messages = getResources().getStringArray(getMessageArray());
        }
        int size = 10;
        List<Data> list = new ArrayList<>(size);
        Data  data;
        for (int i = 0; i < size; i++) {
            data = new Data();
            data.id = idMaker.getAndIncrement();
            data.name = names[data.id % names.length];
            data.avatar = AVATARS[data.id % AVATARS.length];
            data.lastMessage = messages[data.id % messages.length];
            list.add(data);
        }
        return list;
    }

    protected int getMessageArray() {
        return R.array.demo_messages;
    }

    protected class RecyclerAdapter extends RecyclerView.Adapter<ViewHolder> {
        List<Data> list;

        RecyclerAdapter(List<Data> list) {
            this.list = list;
        }

        public void refreshData(List<Data> list) {
            this.list = list;
            notifyDataSetChanged();
        }
        public void addData(List<Data> list) {
            int oldSize = this.list.size();
            this.list.addAll(list);
            notifyItemRangeInserted(oldSize, list.size());
        }

        public void removeItem(int position) {
            if (position >= 0 && position < list.size()) {
                list.remove(position);
                notifyItemRemoved(position);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(BaseRecyclerViewActivity.this).inflate(R.layout.layout_item_view, parent, false);
            return createRecyclerViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.onBindData(list.get(position), position);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }
    protected abstract ViewHolder createRecyclerViewHolder(View view);

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvLastMessage;
        private ImageView ivAvatar;
        private Data data;

        public ViewHolder(View rootView) {
            super(rootView);
            tvName = rootView.findViewById(R.id.tv_name);
            tvLastMessage = rootView.findViewById(R.id.tv_message);
            ivAvatar = rootView.findViewById(R.id.iv_avatar);
            ((View)ivAvatar.getParent()).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (data != null) {
                        Intent intent = new Intent(BaseRecyclerViewActivity.this, ImageViewerActivity.class);
                        intent.putExtra(ImageViewerActivity.IMAGE_EXTRA, data.avatar);
                        startActivity(intent);
                    }
                }
            });
        }

        protected void onBindData(Data data, int position) {
            this.data = data;
            if (data.avatar != 0) {
                ivAvatar.setImageResource(data.avatar);
            }
            tvName.setText(data.name);
            tvLastMessage.setText(data.lastMessage);
        }

    }


    public class Data {
        int id;
        String name;
        String lastMessage;
        int avatar;
    }

    static final int[] AVATARS = new int[] {
            R.drawable.avatar_1
            , R.drawable.avatar_2
            , R.drawable.avatar_3
            , R.drawable.avatar_4
            , R.drawable.avatar_5
            , R.drawable.avatar_6
            , R.drawable.avatar_7
            , R.drawable.avatar_8
            , R.drawable.avatar_9
            , R.drawable.avatar_10
            , R.drawable.avatar_11
            , R.drawable.avatar_12
            , R.drawable.avatar_13
            , R.drawable.avatar_14
            , R.drawable.avatar_15
            , R.drawable.avatar_16
            , R.drawable.avatar_17
            , R.drawable.avatar_18
            , R.drawable.avatar_19
            , R.drawable.avatar_20
    };
}
