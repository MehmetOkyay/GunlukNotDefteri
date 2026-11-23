package com.mehmetokyay.flownote.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.model.BulutProfil;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EngellenenAdapter extends RecyclerView.Adapter<EngellenenAdapter.EngellenenViewHolder> {

    public interface Dinleyici {
        void engeliKaldirIstendi(BulutProfil kullanici);
    }

    private final List<BulutProfil> liste = new ArrayList<>();
    private final Dinleyici dinleyici;

    public EngellenenAdapter(Dinleyici dinleyici) {
        this.dinleyici = dinleyici;
    }

    public void listeyiDegistir(List<BulutProfil> yeniListe) {
        liste.clear();
        if (yeniListe != null) liste.addAll(yeniListe);
        notifyDataSetChanged();
    }

    public void kaldir(String uid) {
        for (int i = 0; i < liste.size(); i++) {
            if (uid.equals(liste.get(i).getUid())) {
                liste.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public static String gorunenAd(BulutProfil u) {
        return BulutSosyal.gorunenAd(u);
    }

    private static String ilkHarf(String metin) {
        if (metin == null || metin.isEmpty()) return "?";
        String temiz = metin.startsWith("@") && metin.length() > 1 ? metin.substring(1) : metin;
        int cp = temiz.codePointAt(0);
        return new String(Character.toChars(cp)).toUpperCase(Locale.getDefault());
    }

    @NonNull
    @Override
    public EngellenenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_engellenen, parent, false);
        return new EngellenenViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EngellenenViewHolder holder, int position) {
        BulutProfil u = liste.get(position);
        String ad = gorunenAd(u);
        String kAdi = u.getKullaniciAdi() != null ? u.getKullaniciAdi().trim() : "";

        holder.textBasHarf.setText(ilkHarf(ad));
        holder.textIsim.setText(ad);
        holder.textKullaniciAdi.setText(kAdi);
        holder.textKullaniciAdi.setVisibility(kAdi.isEmpty() || kAdi.equals(ad) ? View.GONE : View.VISIBLE);

        holder.btnEngeliKaldir.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && dinleyici != null) {
                dinleyici.engeliKaldirIstendi(liste.get(pos));
            }
        });
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class EngellenenViewHolder extends RecyclerView.ViewHolder {
        final TextView textBasHarf, textIsim, textKullaniciAdi;
        final MaterialButton btnEngeliKaldir;

        EngellenenViewHolder(@NonNull View itemView) {
            super(itemView);
            textBasHarf = itemView.findViewById(R.id.textEngellenenBasHarf);
            textIsim = itemView.findViewById(R.id.textEngellenenIsim);
            textKullaniciAdi = itemView.findViewById(R.id.textEngellenenKullaniciAdi);
            btnEngeliKaldir = itemView.findViewById(R.id.btnEngeliKaldir);
        }
    }
}
