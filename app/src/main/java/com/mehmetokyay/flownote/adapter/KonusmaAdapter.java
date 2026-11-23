package com.mehmetokyay.flownote.adapter;

import android.graphics.Typeface;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.KullaniciGorunum;
import com.mehmetokyay.flownote.util.ResimYukleyici;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KonusmaAdapter extends RecyclerView.Adapter<KonusmaAdapter.KonusmaViewHolder> {

    public interface Dinleyici {
        void konusmaSecildi(BulutSosyal.KonusmaOzet konusma);
    }

    private final List<BulutSosyal.KonusmaOzet> liste = new ArrayList<>();
    private final Dinleyici dinleyici;

    public KonusmaAdapter(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void listeyiDegistir(List<BulutSosyal.KonusmaOzet> yeniListe) {
        liste.clear();
        if (yeniListe != null) liste.addAll(yeniListe);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public KonusmaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_konusma, parent, false);
        return new KonusmaViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull KonusmaViewHolder holder, int position) {
        BulutSosyal.KonusmaOzet k = liste.get(position);
        holder.textIsim.setText(k.ad);
        if (k.resimUrl != null && !k.resimUrl.isEmpty()) {
            holder.imageAvatar.setVisibility(View.VISIBLE);
            holder.textBasHarf.setVisibility(View.GONE);
            ResimYukleyici.goster(holder.imageAvatar, k.resimUrl);
        } else {
            holder.imageAvatar.setVisibility(View.GONE);
            holder.textBasHarf.setVisibility(View.VISIBLE);
            holder.textBasHarf.setText(KullaniciGorunum.ilkHarf(k.ad));
        }

        String metin = k.sonMesaj == null ? "" : k.sonMesaj;
        if (k.sonBenim) metin = holder.itemView.getContext().getString(R.string.sen_onek, metin);
        holder.textSonMesaj.setText(metin);

        long zaman = k.zaman;
        if (DateUtils.isToday(zaman)) {
            holder.textZaman.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(zaman)));
        } else {
            holder.textZaman.setText(DateUtils.formatDateTime(holder.itemView.getContext(), zaman,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH));
        }

        boolean okunmamisVar = k.okunmamis > 0;
        holder.textOkunmamis.setVisibility(okunmamisVar ? View.VISIBLE : View.GONE);
        holder.textOkunmamis.setText(k.okunmamis > 99 ? "99+" : String.valueOf(k.okunmamis));
        holder.textSonMesaj.setTypeface(null, okunmamisVar ? Typeface.BOLD : Typeface.NORMAL);
        holder.textSonMesaj.setTextColor(ContextCompat.getColor(holder.itemView.getContext(),
                okunmamisVar ? R.color.renk_metin_baslik : R.color.renk_metin_ikincil));

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && dinleyici != null) dinleyici.konusmaSecildi(liste.get(pos));
        });
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class KonusmaViewHolder extends RecyclerView.ViewHolder {
        final TextView textIsim, textSonMesaj, textZaman, textOkunmamis, textBasHarf;
        final ImageView imageAvatar;

        KonusmaViewHolder(@NonNull View itemView) {
            super(itemView);
            textIsim = itemView.findViewById(R.id.textKonusmaIsim);
            textSonMesaj = itemView.findViewById(R.id.textKonusmaSonMesaj);
            textZaman = itemView.findViewById(R.id.textKonusmaZaman);
            textOkunmamis = itemView.findViewById(R.id.textKonusmaOkunmamis);
            textBasHarf = itemView.findViewById(R.id.textAvatarBasHarf);
            imageAvatar = itemView.findViewById(R.id.imageAvatar);
        }
    }
}
