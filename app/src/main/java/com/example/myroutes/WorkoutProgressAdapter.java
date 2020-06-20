package com.example.myroutes;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myroutes.db.mongoClasses.BoulderItem;

import java.util.List;
import java.util.Locale;

import static com.google.android.gms.common.util.WorkSourceUtil.TAG;

public class WorkoutProgressAdapter extends RecyclerView.Adapter<WorkoutProgressAdapter.MyViewHolder> {

    private int[] colors = {0xFF65def1, 0xFF92374d, 0xFFb5bd89, 0xFFdfbe99, 0xFFec9192};
    private List<List<BoulderItem>> setBoulders;
    private int totalBoulders;
    private int maxWidth;

    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView title, completeBoulders, totalBoulders, fraction;
        ProgressBar progressBar;
        MyViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.setTitle);
            progressBar = view.findViewById(R.id.progressBar);
            completeBoulders = view.findViewById(R.id.boulderProgress);
            totalBoulders = view.findViewById(R.id.boulderTotal);
            fraction = view.findViewById(R.id.fraction);
        }
    }

    public WorkoutProgressAdapter(List<List<BoulderItem>> setBoulders, int totalBoulders, int maxWidth) {
        this.setBoulders = setBoulders;
        this.maxWidth = maxWidth;
        this.totalBoulders = totalBoulders;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_view_progress_bar, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        int nBoulders = setBoulders.get(position).size();
        holder.title.setText(String.format(Locale.US, "Set %d", position + 1));
        holder.progressBar.setMax(nBoulders);
        holder.completeBoulders.setText(String.format(Locale.US, "%d", 0));
        holder.totalBoulders.setText(String.format(Locale.US, "%d", nBoulders));

        // Update size of progress bar
        int progressBarWidth = (int) (float) nBoulders * maxWidth / totalBoulders;
        if (progressBarWidth > 50) {
            holder.progressBar.getLayoutParams().width = progressBarWidth;
        }
        // Update colors of views
        final LayerDrawable layers = (LayerDrawable) holder.progressBar.getProgressDrawable();
        layers.getDrawable(2).setColorFilter(colors[position % colors.length], PorterDuff.Mode.SRC_IN);
        int colorDarker = colors[position % colors.length];
        colorDarker = (int) (colorDarker * 0.5);
        layers.getDrawable(0).setColorFilter(colorDarker, PorterDuff.Mode.SRC_IN);
    }

    @Override
    public int getItemCount() {
        return setBoulders.size();
    }
}
