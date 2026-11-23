package com.mehmetokyay.flownote.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.util.BulutSosyal;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MesajAdapter extends RecyclerView.Adapter<MesajAdapter.MesajViewHolder> {

    private static final int TUR_GIDEN = 1;
    private static final int TUR_GELEN = 2;

    private final List<BulutSosyal.SohbetMesaji> liste = new ArrayList<>();
    private final DateFormat saatBicimi = DateFormat.getTimeInstance(DateFormat.SHORT);
    private String aktifUid = "";

    public void listeyiDegistir(String aktifUid, List<BulutSosyal.SohbetMesaji> mesajlar) {
        this.aktifUid = aktifUid == null ? "" : aktifUid;
        liste.clear();
        if (mesajlar != null) liste.addAll(mesajlar);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return aktifUid.equals(liste.get(position).gonderenUid) ? TUR_GIDEN : TUR_GELEN;
    }

    @NonNull
    @Override
    public MesajViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TUR_GIDEN ? R.layout.item_mesaj_giden : R.layout.item_mesaj_gelen;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MesajViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MesajViewHolder holder, int position) {
        BulutSosyal.SohbetMesaji m = liste.get(position);
        holder.textMetin.setText(m.metin);
        String saat = saatBicimi.format(new Date(m.zaman));
        if (getItemViewType(position) == TUR_GIDEN) {
            saat += m.okundu ? "  ✓✓" : "  ✓";
        }
        holder.textSaat.setText(saat);
    }

    @Override
    public int getItemCount() {
        return liste.size();
    }

    static class MesajViewHolder extends RecyclerView.ViewHolder {
        final TextView textMetin, textSaat;

        MesajViewHolder(@NonNull View itemView) {
            super(itemView);
            textMetin = itemView.findViewById(R.id.textMesajMetni);
            textSaat = itemView.findViewById(R.id.textMesajSaat);
        }
    }
}
