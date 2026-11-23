package com.mehmetokyay.flownote.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.model.Yorum;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.mehmetokyay.flownote.util.ResimYukleyici;

import java.util.ArrayList;
import java.util.List;

public class YorumAdapter extends RecyclerView.Adapter<YorumAdapter.YorumViewHolder> {

    public interface Dinleyici {
        void yorumUzunBasildi(Yorum yorum);
    }

    private final List<Yorum> liste = new ArrayList<>();
    private final Dinleyici dinleyici;

    public YorumAdapter(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void listeyiDegistir(List<Yorum> yorumlar, Object yokSay) {
        liste.clear();
        if (yorumlar != null) liste.addAll(yorumlar);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_yorum, parent, false);
        return new YorumViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {
        Yorum y = liste.get(position);
        String ad = y.getYazarAd() != null && !y.getYazarAd().isEmpty()
                ? y.getYazarAd()
                : holder.itemView.getContext().getString(R.string.bilinmeyen_kullanici);
        holder.textYazar.setText(ad);
        String kAdi = y.getYazarKullaniciAdi() == null ? "" : y.getYazarKullaniciAdi();
        holder.textKullaniciAdi.setText(kAdi);
        holder.textKullaniciAdi.setVisibility(kAdi.isEmpty() || kAdi.equals(ad) ? View.GONE : View.VISIBLE);
        holder.textMetin.setText(y.getMetin());
        holder.textZaman.setText(DateUtils.getRelativeTimeSpanString(
                y.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        if (y.getYazarResimUrl() != null && !y.getYazarResimUrl().isEmpty()) {
            holder.imageAvatar.setVisibility(View.VISIBLE);
            holder.textBasHarf.setVisibility(View.GONE);
            ResimYukleyici.goster(holder.imageAvatar, y.getYazarResimUrl());
        } else {
            KullaniciGorunum.avatarBagla(holder.imageAvatar, holder.textBasHarf, null);
            holder.textBasHarf.setText(KullaniciGorunum.ilkHarf(ad));
        }

        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION || dinleyici == null) return false;
            dinleyici.yorumUzunBasildi(liste.get(pos));
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class YorumViewHolder extends RecyclerView.ViewHolder {
        final TextView textYazar, textKullaniciAdi, textZaman, textMetin, textBasHarf;
        final ImageView imageAvatar;

        YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            textYazar = itemView.findViewById(R.id.textYorumYazar);
            textKullaniciAdi = itemView.findViewById(R.id.textYorumKullaniciAdi);
            textZaman = itemView.findViewById(R.id.textYorumZaman);
            textMetin = itemView.findViewById(R.id.textYorumMetni);
            textBasHarf = itemView.findViewById(R.id.textAvatarBasHarf);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
        }
    }
}
