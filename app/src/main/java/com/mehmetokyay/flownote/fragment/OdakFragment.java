package com.mehmetokyay.flownote.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.util.OdakOturumu;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class OdakFragment extends Fragment {

    private static final long ODAK_SURESI = 25 * 60 * 1000L;
    private static final long KISA_MOLA = 5 * 60 * 1000L;
    private static final long UZUN_MOLA = 15 * 60 * 1000L;
    private static final int MAX_OZEL_DAKIKA = 600;

    private TextView tabOdak, tabKisa, tabUzun;
    private TextView textViewSure, textViewOdakModu;
    private TextView buttonEksi, buttonArti;
    private ProgressBar progressBarPomodoro;
    private MaterialCardView buttonBaslat;
    private TextView textButtonBaslat;

    private final OdakOturumu oturum = OdakOturumu.PAYLASILAN;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_odak, container, false);

        tabOdak = view.findViewById(R.id.tabOdak);
        tabKisa = view.findViewById(R.id.tabKisa);
        tabUzun = view.findViewById(R.id.tabUzun);
        textViewSure = view.findViewById(R.id.textViewSure);
        textViewOdakModu = view.findViewById(R.id.textViewOdakModu);
        progressBarPomodoro = view.findViewById(R.id.progressBarPomodoro);
        buttonBaslat = view.findViewById(R.id.buttonBaslat);
        MaterialCardView buttonSifirla = view.findViewById(R.id.buttonSifirla);
        MaterialCardView buttonAtla = view.findViewById(R.id.buttonAtla);
        MaterialCardView buttonKronometre = view.findViewById(R.id.buttonKronometre);
        MaterialCardView buttonOzelSure = view.findViewById(R.id.buttonOzelSure);
        buttonEksi = view.findViewById(R.id.buttonEksi);
        buttonArti = view.findViewById(R.id.buttonArti);
        textButtonBaslat = view.findViewById(R.id.textButtonBaslat);

        oturum.dinleyiciAta(this::ekraniYenile);
        if (!oturum.kuruldu) oturum.modaGec(0, ODAK_SURESI);
        else ekraniYenile();

        tabOdak.setOnClickListener(v -> oturum.modaGec(0, ODAK_SURESI));
        tabKisa.setOnClickListener(v -> oturum.modaGec(1, KISA_MOLA));
        tabUzun.setOnClickListener(v -> oturum.modaGec(2, UZUN_MOLA));

        buttonBaslat.setOnClickListener(v -> {
            if (oturum.calisiyor) oturum.duraklat();
            else oturum.baslat(requireContext());
        });

        buttonSifirla.setOnClickListener(v -> oturum.sifirla());

        buttonAtla.setOnClickListener(v -> {
            if (oturum.mod == 0 || oturum.mod == -1 || oturum.mod == -2) oturum.modaGec(1, KISA_MOLA);
            else oturum.modaGec(0, ODAK_SURESI);
        });

        buttonArti.setOnClickListener(v -> oturum.dakikaKaydir(1));
        buttonEksi.setOnClickListener(v -> oturum.dakikaKaydir(-1));

        buttonOzelSure.setOnClickListener(v -> ozelSureSor());
        buttonKronometre.setOnClickListener(v -> oturum.kronometreyeGec());

        return view;
    }

    @Override
    public void onDestroyView() {
        oturum.dinleyiciAta(null);
        super.onDestroyView();
        tabOdak = tabKisa = tabUzun = null;
        textViewSure = textViewOdakModu = null;
        buttonEksi = buttonArti = null;
        progressBarPomodoro = null;
        buttonBaslat = null;
        textButtonBaslat = null;
    }

    private void ozelSureSor() {
        EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setHint(R.string.focus_custom_hint);
        input.setPadding(60, 40, 60, 40);

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.focus_custom_title)
                .setMessage(R.string.focus_custom_message)
                .setView(input)
                .setPositiveButton(R.string.focus_custom_set, (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (value.isEmpty()) return;
                    int minutes = 0;
                    try {
                        minutes = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                    }
                    if (minutes >= 1 && minutes <= MAX_OZEL_DAKIKA) {
                        oturum.ozel(minutes * 60_000L);
                    } else if (getContext() != null) {
                        android.widget.Toast.makeText(getContext(), R.string.focus_custom_invalid,
                                android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void ekraniYenile() {
        if (textViewSure == null || tabOdak == null) return;
        sekmeleriBoya();
        textViewOdakModu.setText(modBasligi());
        long gosterilen = oturum.kronometre ? oturum.kronometreMs : oturum.kalan;
        int dakika = (int) (gosterilen / 1000) / 60;
        int saniye = (int) (gosterilen / 1000) % 60;
        textViewSure.setText(String.format(Locale.getDefault(), "%02d:%02d", dakika, saniye));
        if (textButtonBaslat != null) {
            textButtonBaslat.setText(oturum.calisiyor ? R.string.btn_duraklat : R.string.btn_baslat);
        }
        if (progressBarPomodoro != null) {
            if (oturum.kronometre || oturum.toplam <= 0) {
                progressBarPomodoro.setProgress(0);
            } else {
                progressBarPomodoro.setProgress((int) ((oturum.kalan * 100f) / oturum.toplam));
            }
        }
    }

    private int modBasligi() {
        if (oturum.mod == 1) return R.string.kisa_mola;
        if (oturum.mod == 2) return R.string.uzun_mola;
        if (oturum.mod == -1) return R.string.ozel_seans;
        if (oturum.mod == -2) return R.string.kronometre_suresiz;
        return R.string.odaklanma_seansi;
    }

    private void sekmeleriBoya() {
        tabOdak.setBackgroundResource(0);
        tabKisa.setBackgroundResource(0);
        tabUzun.setBackgroundResource(0);
        tabOdak.setTextColor(android.graphics.Color.WHITE);
        tabKisa.setTextColor(android.graphics.Color.WHITE);
        tabUzun.setTextColor(android.graphics.Color.WHITE);
        TextView secili = oturum.mod == 1 ? tabKisa : oturum.mod == 2 ? tabUzun : oturum.mod == 0 ? tabOdak : null;
        if (secili == null) return;
        secili.setBackgroundResource(R.drawable.bg_tab_dikdortgen);
        secili.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
        secili.setTextColor(android.graphics.Color.parseColor("#1E285D"));
    }
}
