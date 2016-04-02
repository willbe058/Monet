package com.xpf.me.monet;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xgo on 12/22/15.
 */
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyHolder> {

    private Context mContext;

    private List<String> mData;

    public MyAdapter(Context context) {
        this.mContext = context;
        this.mData = new ArrayList<>();
    }


    public void addData(List<String> url) {
        this.mData = url;
        notifyDataSetChanged();
    }

    @Override
    public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_image, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position) {
        String url = mData.get(position);
        Monet.with(mContext)
                .load(url)
                .resize(100, 100)
                .placeHolder(R.mipmap.ic_launcher)
                .draw(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class MyHolder extends RecyclerView.ViewHolder {

        private ImageView imageView;

        public MyHolder(View itemView) {
            super(itemView);
            imageView = ((ImageView) itemView.findViewById(R.id.image));
        }
    }

}
