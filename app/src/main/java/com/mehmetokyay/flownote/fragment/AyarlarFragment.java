package com.mehmetokyay.flownote.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.mehmetokyay.flownote.EngellenenlerActivity;
import com.mehmetokyay.flownote.LoginActivity;
import com.mehmetokyay.flownote.ProfiliDuzenleActivity;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.DilYoneticisi;
import com.mehmetokyay.flownote.util.TemaYoneticisi;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AyarlarFragment extends Fragment {
    private static final String TAG = "AyarlarFragment";

    private FrameLayout tabAydinlik, tabKaranlik, tabSistem;
    private View badgeAydinlik, badgeKaranlik, badgeSistem;

    private TextView textAyarlarIsim, textAyarlarEmail, textAyarlarBasHarf;

    private TextView textDetayAdSoyad, textDetayKullaniciAdi, textDetayKullaniciMail;

    private static final String[] DIL_ETIKETLERI = {"Türkçe (TR)", "English (EN)", "Deutsch (DE)"};
    private static final String[] DIL_KODLARI = {DilYoneticisi.TURKCE, DilYoneticisi.ENGLISH, DilYoneticisi.DEUTSCH};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ayarlar, container, false);
        final Context context = requireContext();

        textAyarlarIsim = view.findViewById(R.id.textAyarlarIsim);
        textAyarlarEmail = view.findViewById(R.id.textAyarlarEmail);
        textAyarlarBasHarf = view.findViewById(R.id.textAyarlarBasHarf);
        textDetayAdSoyad = view.findViewById(R.id.textDetayAdSoyad);
        textDetayKullaniciAdi = view.findViewById(R.id.textDetayKullaniciAdi);
        textDetayKullaniciMail = view.findViewById(R.id.textDetayKullaniciMail);

        ImageView btnGeri = view.findViewById(R.id.btnGeri);
        if (btnGeri != null) {
            btnGeri.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().getOnBackPressedDispatcher().onBackPressed();
            });
        }

        TextView btnYonet = view.findViewById(R.id.btnYonet);
        if (btnYonet != null) {
            btnYonet.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), ProfiliDuzenleActivity.class));
            });
        }

        tabAydinlik = view.findViewById(R.id.tabAydinlik);
        tabKaranlik = view.findViewById(R.id.tabKaranlik);
        tabSistem = view.findViewById(R.id.tabSistem);
        badgeAydinlik = view.findViewById(R.id.badgeAydinlik);
        badgeKaranlik = view.findViewById(R.id.badgeKaranlik);
        badgeSistem = view.findViewById(R.id.badgeSistem);

        setThemeTab(TemaYoneticisi.temaModuGetir(context), false);

        if (tabAydinlik != null) tabAydinlik.setOnClickListener(v -> setThemeTab(TemaYoneticisi.MOD_AYDINLIK, true));
        if (tabKaranlik != null) tabKaranlik.setOnClickListener(v -> setThemeTab(TemaYoneticisi.MOD_KARANLIK, true));
        if (tabSistem != null) tabSistem.setOnClickListener(v -> setThemeTab(TemaYoneticisi.MOD_SISTEM, true));

        RelativeLayout btnDilSecimi = view.findViewById(R.id.btnDilSecimi);
        TextView textSeciliDil = view.findViewById(R.id.textSeciliDil);

        int seciliDilIndex = dilIndeksiBul(DilYoneticisi.seciliDiliGetir(context));
        if (textSeciliDil != null) {
            textSeciliDil.setText(DIL_ETIKETLERI[seciliDilIndex] + " ▼");
        }

        if (btnDilSecimi != null && textSeciliDil != null) {
            btnDilSecimi.setOnClickListener(v -> {
                Context ctx = getContext();
                if (ctx == null) return;
                int checkedItem = dilIndeksiBul(DilYoneticisi.seciliDiliGetir(ctx));

                new android.app.AlertDialog.Builder(ctx)
                        .setTitle(R.string.language_dialog_title)
                        .setSingleChoiceItems(DIL_ETIKETLERI, checkedItem, (dialog, which) -> {
                            textSeciliDil.setText(DIL_ETIKETLERI[which] + " ▼");
                            dialog.dismiss();
                            DilYoneticisi.diliDegistir(ctx, DIL_KODLARI[which]);
                        }).show();
            });
        }

        ayarlarKullaniciBilgileriniGetir();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        RelativeLayout rowHesapBilgileri = view.findViewById(R.id.rowHesapBilgileri);
        LinearLayout layoutHesapDetay = view.findViewById(R.id.layoutHesapDetay);
        TextView btnHesabiSil = view.findViewById(R.id.btnHesabiSil);

        RelativeLayout rowSifre2FA = view.findViewById(R.id.rowSifre2FA);
        LinearLayout layoutSifreDetay = view.findViewById(R.id.layoutSifreDetay);
        MaterialButton btnSifreSifirla = view.findViewById(R.id.btnSifreSifirla);

        RelativeLayout rowGizlilik = view.findViewById(R.id.rowGizlilik);

        if (rowHesapBilgileri != null) {
            rowHesapBilgileri.setOnClickListener(v -> toggleGörünürlük(layoutHesapDetay));
        }

        if (btnHesabiSil != null) {
            btnHesabiSil.setOnClickListener(v -> hesabiTamamenSil());
        }

        if (rowSifre2FA != null) {
            rowSifre2FA.setOnClickListener(v -> toggleGörünürlük(layoutSifreDetay));
        }

        if (btnSifreSifirla != null) {
            btnSifreSifirla.setOnClickListener(v -> {
                if (currentUser != null && currentUser.getEmail() != null) {
                    final Context appContext = context.getApplicationContext();
                    FirebaseAuth.getInstance().sendPasswordResetEmail(currentUser.getEmail())
                            .addOnCompleteListener(task -> Toast.makeText(appContext,
                                    task.isSuccessful() ? R.string.sifre_mail_gonderildi : R.string.hata_tekrar_deneyin,
                                    task.isSuccessful() ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show());
                }
            });
        }

        if (rowGizlilik != null) {
            rowGizlilik.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), EngellenenlerActivity.class));
            });
        }

        MaterialButton btnCikisYap = view.findViewById(R.id.btnCikisYap);
        if (btnCikisYap != null) {
            btnCikisYap.setOnClickListener(v -> {
                v.setEnabled(false);
                cikisYap(v.getContext().getApplicationContext());
            });
        }

        return view;
    }

    private void toggleGörünürlük(View view) {
        if (view == null) return;
        if (view.getVisibility() == View.GONE) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void hesabiTamamenSil() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        final Context context = getContext();
        if (user == null || context == null) return;
        final Context appContext = context.getApplicationContext();

        new android.app.AlertDialog.Builder(context)
                .setTitle(getString(R.string.hesabi_sil))
                .setMessage(getString(R.string.hesabi_sil_mesaj))
                .setPositiveButton(getString(R.string.hesabi_sil), (dialog, which) -> {
                    BulutSosyal.hesabiSil(user.getUid(), (bulut, bulutHata) -> {
                        if (!Boolean.TRUE.equals(bulut)) {
                            if (isAdded()) {
                                Toast.makeText(appContext, R.string.silme_hatasi, Toast.LENGTH_LONG).show();
                            }
                            return;
                        }
                        user.delete().addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                if (isAdded()) {
                                    Toast.makeText(appContext, R.string.silme_hatasi, Toast.LENGTH_LONG).show();
                                }
                                return;
                            }
                            TaskDatabase.ARKA_PLAN.execute(() -> {
                                try {
                                    yerelVerileriTemizle(appContext);
                                } catch (Exception e) {
                                    Log.e(TAG, "Yerel veriler temizlenemedi", e);
                                }
                                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(appContext, R.string.hesabiniz_silindi, Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(appContext, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    appContext.startActivity(intent);
                                });
                            });
                        });
                    });
                })
                .setNegativeButton(getString(R.string.iptal), null)
                .show();
    }

    private static void yerelVerileriTemizle(Context appContext) {
        TaskDatabase.getInstance(appContext).clearAllTables();
        java.io.File[] dosyalar = appContext.getFilesDir().listFiles();
        if (dosyalar != null) {
            for (java.io.File d : dosyalar) {
                if (d.isFile() && d.getName().startsWith("profil_")) {
                    d.delete();
                }
            }
        }
    }

    private void cikisYap(Context appContext) {
        FirebaseAuth.getInstance().signOut();

        Runnable girisEkraninaDon = () -> {
            Toast.makeText(appContext, R.string.logout_success, Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(appContext, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        };

        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(appContext, gso);
            googleSignInClient.signOut().addOnCompleteListener(task -> girisEkraninaDon.run());
        } catch (Exception e) {
            Log.e(TAG, "Google çıkışı yapılamadı", e);
            girisEkraninaDon.run();
        }
    }

    private int dilIndeksiBul(String dilKodu) {
        for (int i = 0; i < DIL_KODLARI.length; i++) {
            if (DIL_KODLARI[i].equals(dilKodu)) return i;
        }
        return 0;
    }

    private void ayarlarKullaniciBilgileriniGetir() {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final Context context = getContext();
        if (currentUser == null || context == null) return;
        final Context appContext = context.getApplicationContext();

        ekranaYaz(null, currentUser.getEmail(), currentUser.getDisplayName());

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                final User aktifKullanici = YerelKullanici.garantiEt(appContext, currentUser);
                if (aktifKullanici == null) return;
                final View kok = getView();
                if (kok == null) return;
                kok.post(() -> {
                    if (!isAdded()) return;
                    ekranaYaz(aktifKullanici, currentUser.getEmail(), currentUser.getDisplayName());
                });
            } catch (Exception e) {
                Log.e(TAG, "Kullanıcı bilgisi okunamadı", e);
            }
        });
    }

    private void ekranaYaz(@Nullable User kullanici, @Nullable String eposta, @Nullable String firebaseAd) {
        String isim = "";
        String soyisim = "";
        if (kullanici != null) {
            isim = kullanici.getUserIsim() != null ? kullanici.getUserIsim().trim() : "";
            soyisim = kullanici.getUserSoyisim() != null ? kullanici.getUserSoyisim().trim() : "";
        }

        if (isim.isEmpty() && soyisim.isEmpty()) {
            if (firebaseAd != null && !firebaseAd.trim().isEmpty()) {
                isim = firebaseAd.trim();
            } else if (eposta != null && eposta.contains("@")) {
                isim = eposta.substring(0, eposta.indexOf('@'));
            }
        }

        String tamAd = (isim + " " + soyisim).trim();

        if (textAyarlarIsim != null) textAyarlarIsim.setText(tamAd);
        if (textAyarlarEmail != null) textAyarlarEmail.setText(eposta != null ? eposta : "");

        if (textAyarlarBasHarf != null) {
            String basHarf = "";
            if (!isim.isEmpty()) basHarf += isim.substring(0, 1);
            if (!soyisim.isEmpty()) basHarf += soyisim.substring(0, 1);
            textAyarlarBasHarf.setText(basHarf.toUpperCase(java.util.Locale.getDefault()));
        }

        if (textDetayAdSoyad != null) textDetayAdSoyad.setText(tamAd);
        if (textDetayKullaniciAdi != null) {
            String kAdi = kullanici != null && kullanici.getKullaniciAdi() != null ? kullanici.getKullaniciAdi() : "";
            textDetayKullaniciAdi.setText(kAdi);
            textDetayKullaniciAdi.setVisibility(kAdi.isEmpty() ? View.GONE : View.VISIBLE);
        }
        if (textDetayKullaniciMail != null) textDetayKullaniciMail.setText(eposta != null ? eposta : "");
    }

    private void setThemeTab(int mod, boolean applyTheme) {
        resetTab(tabAydinlik, badgeAydinlik);
        resetTab(tabKaranlik, badgeKaranlik);
        resetTab(tabSistem, badgeSistem);

        if (mod == TemaYoneticisi.MOD_AYDINLIK) setActiveTab(tabAydinlik, badgeAydinlik);
        else if (mod == TemaYoneticisi.MOD_KARANLIK) setActiveTab(tabKaranlik, badgeKaranlik);
        else setActiveTab(tabSistem, badgeSistem);

        Context context = getContext();
        if (applyTheme && context != null) {
            TemaYoneticisi.temaModunuDegistir(context, mod);
        }
    }

    private void resetTab(FrameLayout tab, View badge) {
        Context context = getContext();
        if (tab == null || badge == null || context == null) return;
        tab.setBackgroundResource(0);
        View ilk = tab.getChildAt(0);
        if (ilk instanceof LinearLayout) {
            LinearLayout inner = (LinearLayout) ilk;
            if (inner.getChildCount() > 1
                    && inner.getChildAt(0) instanceof ImageView
                    && inner.getChildAt(1) instanceof TextView) {
                ImageView icon = (ImageView) inner.getChildAt(0);
                TextView text = (TextView) inner.getChildAt(1);
                int renk = ContextCompat.getColor(context, R.color.renk_metin_ikincil);
                icon.setColorFilter(renk);
                text.setTextColor(renk);
                text.setTypeface(null, Typeface.NORMAL);
            }
        }
        badge.setVisibility(View.GONE);
    }

    private void setActiveTab(FrameLayout tab, View badge) {
        Context context = getContext();
        if (tab == null || badge == null || context == null) return;
        tab.setBackgroundResource(R.drawable.bg_tab_dikdortgen);
        tab.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.renk_vurgu)));
        View ilk = tab.getChildAt(0);
        if (ilk instanceof LinearLayout) {
            LinearLayout inner = (LinearLayout) ilk;
            if (inner.getChildCount() > 1
                    && inner.getChildAt(0) instanceof ImageView
                    && inner.getChildAt(1) instanceof TextView) {
                ImageView icon = (ImageView) inner.getChildAt(0);
                TextView text = (TextView) inner.getChildAt(1);
                icon.setColorFilter(Color.WHITE);
                text.setTextColor(Color.WHITE);
                text.setTypeface(null, Typeface.BOLD);
            }
        }
        badge.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        tabAydinlik = tabKaranlik = tabSistem = null;
        badgeAydinlik = badgeKaranlik = badgeSistem = null;
        textAyarlarIsim = textAyarlarEmail = textAyarlarBasHarf = null;
        textDetayAdSoyad = textDetayKullaniciAdi = textDetayKullaniciMail = null;
    }
}