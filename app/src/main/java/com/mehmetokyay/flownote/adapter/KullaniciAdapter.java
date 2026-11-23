package com.mehmetokyay.flownote.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.model.BulutProfil;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.mehmetokyay.flownote.util.ResimYukleyici;

import java.util.ArrayList;
import java.util.List;

public class KullaniciAdapter extends RecyclerView.Adapter<KullaniciAdapter.KullaniciViewHolder> {

    public interface Dinleyici {
        void kullaniciSecildi(BulutProfil kullanici);
    }

    private final List<BulutProfil> liste = new ArrayList<>();
    private final Dinleyici dinleyici;

    public KullaniciAdapter(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void listeyiDegistir(List<BulutProfil> yeniListe) {
        liste.clear();
        if (yeniListe != null) liste.addAll(yeniListe);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KullaniciViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kullanici, parent, false);
        return new KullaniciViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KullaniciViewHolder holder, int position) {
        BulutProfil u = liste.get(position);
        String ad = BulutSosyal.gorunenAd(u);
        String kAdi = u.getKullaniciAdi() == null ? "" : u.getKullaniciAdi();
        holder.textIsim.setText(ad);
        holder.textKullaniciAdi.setText(kAdi);
        holder.textKullaniciAdi.setVisibility(kAdi.isEmpty() || kAdi.equals(ad) ? View.GONE : View.VISIBLE);
        if (u.getProfilResmiUrl() != null && !u.getProfilResmiUrl().isEmpty()) {
            holder.imageAvatar.setVisibility(View.VISIBLE);
            holder.textBasHarf.setVisibility(View.GONE);
            ResimYukleyici.goster(holder.imageAvatar, u.getProfilResmiUrl());
        } else {
            holder.imageAvatar.setVisibility(View.GONE);
            holder.textBasHarf.setVisibility(View.VISIBLE);
            holder.textBasHarf.setText(KullaniciGorunum.ilkHarf(ad));
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && dinleyici != null) dinleyici.kullaniciSecildi(liste.get(pos));
        });
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class KullaniciViewHolder extends RecyclerView.ViewHolder {
        final TextView textIsim, textKullaniciAdi, textBasHarf;
        final ImageView imageAvatar;

        KullaniciViewHolder(@NonNull View itemView) {
            super(itemView);
            textIsim = itemView.findViewById(R.id.textKullaniciIsim);
            textKullaniciAdi = itemView.findViewById(R.id.textKullaniciKullaniciAdi);
            textBasHarf = itemView.findViewById(R.id.textAvatarBasHarf);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
        }
    }
}
