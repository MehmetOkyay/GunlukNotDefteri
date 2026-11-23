package com.mehmetokyay.flownote.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.util.ResimYukleyici;

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postListesi;

    public PostAdapter(List<Post> postListesi) {
        this.postListesi = postListesi;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postListesi.get(position);

        holder.textBaslik.setText(post.getBaslik() != null ? post.getBaslik() : "");
        holder.textIcerik.setText(post.getIcerik() != null ? post.getIcerik() : "");
        holder.textBegeni.setText("❤ " + post.getBegeniSayisi());
        holder.textYorum.setText("💬 " + post.getYorumSayisi());

        if (post.getResimUri() != null && !post.getResimUri().isEmpty()) {
            holder.imagePostImage.setImageTintList(null);
            ResimYukleyici.goster(holder.imagePostImage, post.getResimUri());
        } else {
            holder.imagePostImage.setImageResource(android.R.drawable.ic_menu_camera);
            holder.imagePostImage.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#888888")));
        }
    }

    @Override
    public int getItemCount() {
        return postListesi != null ? postListesi.size() : 0;
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView textBaslik, textIcerik, textBegeni, textYorum;
        ImageView imagePostImage;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            textBaslik = itemView.findViewById(R.id.textPostTitle);
            textIcerik = itemView.findViewById(R.id.textPostContent);
            textBegeni = itemView.findViewById(R.id.textPostLikes);
            textYorum = itemView.findViewById(R.id.textPostComments);
            imagePostImage = itemView.findViewById(R.id.imagePostImage);
        }
    }
}