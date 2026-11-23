package com.mehmetokyay.flownote.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.util.BildirimMerkezi;

import java.util.ArrayList;
import java.util.List;

public class BildirimAdapter extends RecyclerView.Adapter<BildirimAdapter.Tutucu> {

    public interface Dinleyici {
        void bildirimTiklandi(Oge oge);
    }

    public static class Oge {
        public String id = "";
        public String tip = "";
        public String gonderenUid = "";
        public String gonderenAd = "";
        public String metin = "";
        public String ilgiliId = "";
        public long zaman;
        public boolean okundu;
    }

    private final List<Oge> liste = new ArrayList<>();
    private final Dinleyici dinleyici;

    public BildirimAdapter(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void listeyiDegistir(List<Oge> yeni) {
        liste.clear();
        if (yeni != null) liste.addAll(yeni);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Tutucu onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bildirim, parent, false);
        return new Tutucu(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Tutucu holder, int position) {
        Oge oge = liste.get(position);
        String ad = oge.gonderenAd == null || oge.gonderenAd.isEmpty()
                ? holder.itemView.getContext().getString(R.string.bilinmeyen_kullanici)
                : oge.gonderenAd;
        int sablon;
        if (BildirimMerkezi.TIP_TAKIP.equals(oge.tip)) sablon = R.string.bildirim_takip;
        else if (BildirimMerkezi.TIP_BEGENI.equals(oge.tip)) sablon = R.string.bildirim_begeni;
        else if (BildirimMerkezi.TIP_YORUM.equals(oge.tip)) sablon = R.string.bildirim_yorum;
        else if (BildirimMerkezi.TIP_MESAJ.equals(oge.tip)) sablon = R.string.bildirim_mesaj;
        else sablon = R.string.bildirimler;
        holder.textBaslik.setText(sablon == R.string.bildirimler
                ? ad
                : holder.itemView.getContext().getString(sablon, ad));
        holder.textMetin.setText(oge.metin == null ? "" : oge.metin);
        holder.textMetin.setVisibility(oge.metin == null || oge.metin.isEmpty() ? View.GONE : View.VISIBLE);
        holder.textZaman.setText(oge.zaman > 0
                ? DateUtils.getRelativeTimeSpanString(oge.zaman, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                : "");
        int renk = ContextCompat.getColor(holder.itemView.getContext(),
                oge.okundu ? R.color.renk_kart : R.color.renk_arkaplan);
        holder.itemView.setBackgroundColor(renk);
        holder.itemView.setOnClickListener(v -> {
            if (dinleyici != null) dinleyici.bildirimTiklandi(oge);
        });
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class Tutucu extends RecyclerView.ViewHolder {
        final TextView textBaslik, textMetin, textZaman;

        Tutucu(@NonNull View itemView) {
            super(itemView);
            textBaslik = itemView.findViewById(R.id.textBildirimBaslik);
            textMetin = itemView.findViewById(R.id.textBildirimMetin);
            textZaman = itemView.findViewById(R.id.textBildirimZaman);
        }
    }
}
