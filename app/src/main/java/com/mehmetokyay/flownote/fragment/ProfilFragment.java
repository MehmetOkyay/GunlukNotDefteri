package com.mehmetokyay.flownote.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.ProfiliDuzenleActivity;
import com.mehmetokyay.flownote.BildirimlerActivity;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.adapter.PostAdapter;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.ResimYukleyici;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProfilFragment extends Fragment {
    private static final String TAG = "ProfilFragment";

    private TextView textProfilTamAd, textProfilKullaniciAdi, textProfilMeslek, textProfilKonum, textProfilBiyografi;
    private TextView textPaylasimSayisi, textTakipciSayisi, textTakipEdilenSayisi;
    private ImageView imageProfilResmi;

    private LinearLayout tabGonderiler, tabBegenilenler;
    private ImageView iconGonderiler, iconBegenilenler;
    private TextView textTabGonderiler, textTabBegenilenler;
    private View lineGonderiler, lineBegenilenler;

    private RecyclerView recyclerGonderiler;
    private LinearLayout layoutBosDurum;
    private TextView textBosDurum;

    private PostAdapter postAdapter;
    private List<Post> gonderiListesi;
    private boolean begenilenSekmesi = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profil, container, false);

        textProfilTamAd = view.findViewById(R.id.textProfilTamAd);
        textProfilKullaniciAdi = view.findViewById(R.id.textProfilKullaniciAdi);
        textProfilMeslek = view.findViewById(R.id.textProfilMeslek);
        textProfilKonum = view.findViewById(R.id.textProfilKonum);
        textProfilBiyografi = view.findViewById(R.id.textProfilBiyografi);
        textPaylasimSayisi = view.findViewById(R.id.textPaylasimSayisi);
        textTakipciSayisi = view.findViewById(R.id.textTakipciSayisi);
        textTakipEdilenSayisi = view.findViewById(R.id.textTakipEdilenSayisi);
        imageProfilResmi = view.findViewById(R.id.imageProfilResmi);

        layoutBosDurum = view.findViewById(R.id.layoutBosDurum);
        textBosDurum = view.findViewById(R.id.textBosDurum);

        tabGonderiler = view.findViewById(R.id.tabGonderiler);
        tabBegenilenler = view.findViewById(R.id.tabBegenilenler);
        iconGonderiler = view.findViewById(R.id.iconGonderiler);
        iconBegenilenler = view.findViewById(R.id.iconBegenilenler);
        textTabGonderiler = view.findViewById(R.id.textTabGonderiler);
        textTabBegenilenler = view.findViewById(R.id.textTabBegenilenler);
        lineGonderiler = view.findViewById(R.id.lineGonderiler);
        lineBegenilenler = view.findViewById(R.id.lineBegenilenler);

        recyclerGonderiler = view.findViewById(R.id.recyclerGonderiler);
        recyclerGonderiler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        gonderiListesi = new ArrayList<>();
        postAdapter = new PostAdapter(gonderiListesi);
        recyclerGonderiler.setAdapter(postAdapter);

        tabGonderiler.setOnClickListener(v -> sekmeDegistir(true));
        tabBegenilenler.setOnClickListener(v -> sekmeDegistir(false));

        View iconAyarlar = view.findViewById(R.id.iconAyarlar);
        View iconBildirimler = view.findViewById(R.id.iconBildirimler);
        if (iconBildirimler != null) {
            iconBildirimler.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), BildirimlerActivity.class)));
        }
        if (iconAyarlar != null) {
            iconAyarlar.setOnClickListener(v -> {
                if (!isAdded()) return;
                FragmentManager fm = requireActivity().getSupportFragmentManager();
                if (fm.isStateSaved()) return;
                FragmentTransaction transaction = fm.beginTransaction();
                transaction.replace(R.id.fragment_container, new AyarlarFragment());
                transaction.addToBackStack(null);
                transaction.commit();
            });
        }

        View btnProfiliDuzenle = view.findViewById(R.id.btnProfiliDuzenle);
        if (btnProfiliDuzenle != null) {
            btnProfiliDuzenle.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), ProfiliDuzenleActivity.class);
                startActivity(intent);
            });
        }

        View btnPaylas = view.findViewById(R.id.btnPaylas);
        if (btnPaylas != null) {
            btnPaylas.setOnClickListener(v -> {
                String aktifKullaniciAdi = textProfilKullaniciAdi != null ? textProfilKullaniciAdi.getText().toString() : "";
                String temizKullaniciAdi = aktifKullaniciAdi.replace("@", "");
                String profilLinki = "flownote://profil/" + temizKullaniciAdi;
                String paylasimMetni = getString(R.string.profil_paylas_metin, profilLinki);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.profil_paylas_konu));
                shareIntent.putExtra(Intent.EXTRA_TEXT, paylasimMetni);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.profil_paylas_baslik)));
            });
        }

        ekranaYaz(null, FirebaseAuth.getInstance().getCurrentUser(), 0);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        kullaniciBilgileriniGetir();
        if (begenilenSekmesi) begenilenleriGetir();
    }

    private void sekmeDegistir(boolean isGonderilerTab) {
        int aktifRenk = Color.parseColor("#1756D3");
        int pasifRenk = Color.parseColor("#888888");
        begenilenSekmesi = !isGonderilerTab;

        if (isGonderilerTab) {
            iconGonderiler.setColorFilter(aktifRenk);
            textTabGonderiler.setTextColor(aktifRenk);
            lineGonderiler.setBackgroundColor(aktifRenk);
            iconBegenilenler.setColorFilter(pasifRenk);
            textTabBegenilenler.setTextColor(pasifRenk);
            lineBegenilenler.setBackgroundColor(Color.TRANSPARENT);

            bosDurumGuncelle(true, getString(R.string.henuz_gonderi_yok));

            kullaniciBilgileriniGetir();
        } else {
            iconBegenilenler.setColorFilter(aktifRenk);
            textTabBegenilenler.setTextColor(aktifRenk);
            lineBegenilenler.setBackgroundColor(aktifRenk);
            iconGonderiler.setColorFilter(pasifRenk);
            textTabGonderiler.setTextColor(pasifRenk);
            lineGonderiler.setBackgroundColor(Color.TRANSPARENT);

            gonderiListesi.clear();
            postAdapter.notifyDataSetChanged();

            bosDurumGuncelle(true, getString(R.string.begenilen_gonderi_yok));
            begenilenleriGetir();
        }
    }

    private void begenilenleriGetir() {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final Context context = getContext();
        if (currentUser == null || context == null) return;
        final Context appContext = context.getApplicationContext();

        BulutSosyal.begenilenGonderiler(currentUser.getUid(), (akis, hata) -> {
            if (!isAdded() || getView() == null || !begenilenSekmesi) return;
            if (hata != null || akis == null) {
                Log.e(TAG, "Beğenilen gönderiler okunamadı", hata);
                return;
            }
            gonderiListesi.clear();
            gonderiListesi.addAll(akis.gonderiler);
            postAdapter.notifyDataSetChanged();
            bosDurumGuncelle(akis.gonderiler.isEmpty(), getString(R.string.begenilen_gonderi_yok));
        });
    }

    private void kullaniciBilgileriniGetir() {
        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        final Context context = getContext();
        if (currentUser == null || context == null) return;
        final Context appContext = context.getApplicationContext();

        BulutSosyal.takipSayilari(currentUser.getUid(), (sayi, hata) -> {
            if (!isAdded() || getView() == null || sayi == null) return;
            if (textTakipciSayisi != null) textTakipciSayisi.setText(takipciyiBicimle(sayi[0]));
            if (textTakipEdilenSayisi != null) textTakipEdilenSayisi.setText(takipciyiBicimle(sayi[1]));
        });

        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                final User aktifKullanici = YerelKullanici.garantiEt(appContext, currentUser);
                final View kok = getView();
                if (kok == null || aktifKullanici == null) return;
                kok.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    ekranaYaz(aktifKullanici, currentUser, 0);
                });
            } catch (Exception e) {
                Log.e(TAG, "Profil bilgisi okunamadı", e);
            }
        });

        BulutSosyal.kullaniciGonderileri(currentUser.getUid(), currentUser.getUid(), (akis, hata) -> {
            if (!isAdded() || getView() == null) return;
            if (hata != null || akis == null) {
                Log.e(TAG, "Gönderiler okunamadı", hata);
                return;
            }
            if (textPaylasimSayisi != null) textPaylasimSayisi.setText(String.valueOf(akis.gonderiler.size()));
            if (begenilenSekmesi) return;
            gonderiListesi.clear();
            gonderiListesi.addAll(akis.gonderiler);
            postAdapter.notifyDataSetChanged();
            bosDurumGuncelle(akis.gonderiler.isEmpty(), getString(R.string.henuz_gonderi_yok));
        });
    }

    private void bosDurumGuncelle(boolean bosMu, String mesaj) {
        if (recyclerGonderiler == null || layoutBosDurum == null || textBosDurum == null) return;
        if (bosMu) {
            recyclerGonderiler.setVisibility(View.GONE);
            layoutBosDurum.setVisibility(View.VISIBLE);
            textBosDurum.setText(mesaj);
        } else {
            recyclerGonderiler.setVisibility(View.VISIBLE);
            layoutBosDurum.setVisibility(View.GONE);
        }
    }

    private Bitmap guvenliBitmapYukle(String dosyaYolu, int hedefGenislik, int hedefYukseklik) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(dosyaYolu, options);

            options.inSampleSize = hesaplaInSampleSize(options, hedefGenislik, hedefYukseklik);
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(dosyaYolu, options);
        } catch (Exception e) {
            Log.e(TAG, "Bitmap yükleme hatası", e);
            return null;
        }
    }

    private int hesaplaInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void ekranaYaz(@Nullable User kullanici, @Nullable FirebaseUser firebaseKullanici, int paylasimSayisi) {
        String isim = "";
        String soyisim = "";
        if (kullanici != null) {
            isim = bosDegilse(kullanici.getUserIsim());
            soyisim = bosDegilse(kullanici.getUserSoyisim());

            if (imageProfilResmi != null) {
                String resim = kullanici.getProfilResmi();
                if (resim != null && resim.startsWith("http")) {
                    imageProfilResmi.setImageTintList(null);
                    imageProfilResmi.clearColorFilter();
                    ResimYukleyici.goster(imageProfilResmi, resim);
                } else if (resim != null && !resim.isEmpty()) {
                    try {
                        File imgFile = new File(resim);
                        if (imgFile.exists()) {
                            Bitmap bitmap = guvenliBitmapYukle(imgFile.getAbsolutePath(), 300, 300);
                            if (bitmap != null) {
                                imageProfilResmi.setImageTintList(null);
                                imageProfilResmi.clearColorFilter();
                                imageProfilResmi.setImageBitmap(bitmap);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Profil resmi yüklenemedi", e);
                    }
                } else {
                    imageProfilResmi.setImageResource(android.R.drawable.ic_menu_camera);
                    imageProfilResmi.setColorFilter(Color.parseColor("#888888"));
                }
            }
        }

        String eposta = firebaseKullanici != null ? firebaseKullanici.getEmail() : null;
        String epostaKisim = eposta != null && eposta.contains("@") ? eposta.substring(0, eposta.indexOf('@')) : "";
        if (isim.isEmpty() && soyisim.isEmpty()) {
            String firebaseAd = firebaseKullanici != null ? bosDegilse(firebaseKullanici.getDisplayName()) : "";
            isim = !firebaseAd.isEmpty() ? firebaseAd : epostaKisim;
        }
        String tamAd = (isim + " " + soyisim).trim();

        String kullaniciAdi = kullanici != null ? bosDegilse(kullanici.getKullaniciAdi()) : "";
        if (kullaniciAdi.isEmpty()) {
            String govde = (isim + "." + soyisim).replaceAll("\\s+", "").toLowerCase(Locale.getDefault());
            if (govde.endsWith(".")) govde = govde.substring(0, govde.length() - 1);
            kullaniciAdi = govde.isEmpty() ? "" : "@" + govde;
        }

        if (textProfilTamAd != null) textProfilTamAd.setText(tamAd);
        if (textProfilKullaniciAdi != null) textProfilKullaniciAdi.setText(kullaniciAdi);

        metinVeyaGizle(textProfilMeslek, kullanici != null ? bosDegilse(kullanici.getMeslek()) : "");
        metinVeyaGizle(textProfilBiyografi, kullanici != null ? bosDegilse(kullanici.getBiyografi()) : "");

        if (textProfilKonum != null) {
            String konum = kullanici != null ? bosDegilse(kullanici.getKonum()) : "";
            String web = kullanici != null ? bosDegilse(kullanici.getWebSitesi()) : "";
            StringBuilder sb = new StringBuilder();
            if (!konum.isEmpty()) sb.append("📍 ").append(konum);
            if (!web.isEmpty()) {
                if (sb.length() > 0) sb.append("  •  ");
                sb.append("🔗 ").append(web);
            }
            metinVeyaGizle(textProfilKonum, sb.toString());
        }

        if (textPaylasimSayisi != null) {
            textPaylasimSayisi.setText(String.valueOf(paylasimSayisi));
        }
    }

    private static String takipciyiBicimle(int sayi) {
        if (sayi < 1000) return String.valueOf(sayi);
        return String.format(Locale.US, "%.1fk", sayi / 1000f).replace(".0k", "k");
    }

    private static String bosDegilse(String s) {
        return s == null ? "" : s.trim();
    }

    private static void metinVeyaGizle(@Nullable TextView tv, String metin) {
        if (tv == null) return;
        tv.setText(metin);
        tv.setVisibility(metin.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        textProfilTamAd = textProfilKullaniciAdi = textProfilMeslek = textProfilKonum = textProfilBiyografi = null;
        textPaylasimSayisi = textTakipciSayisi = textTakipEdilenSayisi = null;
        imageProfilResmi = null;
        recyclerGonderiler = null;
        layoutBosDurum = null;
        textBosDurum = null;
        tabGonderiler = tabBegenilenler = null;
        iconGonderiler = iconBegenilenler = null;
        textTabGonderiler = textTabBegenilenler = null;
        lineGonderiler = lineBegenilenler = null;
    }
}