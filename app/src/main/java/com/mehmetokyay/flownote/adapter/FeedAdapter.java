package com.mehmetokyay.flownote.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mehmetokyay.flownote.KullaniciProfilActivity;
import com.mehmetokyay.flownote.R;
import com.mehmetokyay.flownote.SohbetActivity;
import com.mehmetokyay.flownote.fragment.YorumlarBottomSheet;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.util.BulutSosyal;
import com.mehmetokyay.flownote.util.ResimYukleyici;
import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.util.YerelKullanici;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    private static final String TAG = "FeedAdapter";
    private static final Object PAYLOAD_SAYAC = new Object();

    private static final int MENU_PROFIL = 1;
    private static final int MENU_MESAJ = 2;
    private static final int MENU_PAYLAS = 3;
    private static final int MENU_KOPYALA = 4;
    private static final int MENU_ENGELLE = 5;
    private static final int MENU_SIL = 6;
    private static final int MENU_SIKAYET = 7;

    private final FragmentActivity activity;
    private final List<Post> postList = new ArrayList<>();
    private final Set<String> begenilenler = new HashSet<>();
    private String aktifUid = "";
    private boolean profilAcilabilir = true;
    private Runnable degisiklikDinleyici;

    public FeedAdapter(FragmentActivity activity) {
        this.activity = activity;
    }

    public void veriyiAyarla(String uid, BulutSosyal.Akis veri) {
        aktifUid = uid == null ? "" : uid;
        postList.clear();
        begenilenler.clear();
        if (veri != null) {
            postList.addAll(veri.gonderiler);
            begenilenler.addAll(veri.begenilenler);
        }
        notifyDataSetChanged();
    }

    public void temizle() {
        postList.clear();
        notifyDataSetChanged();
    }

    public void setProfilAcilabilir(boolean profilAcilabilir) {
        this.profilAcilabilir = profilAcilabilir;
    }

    public void setDegisiklikDinleyici(Runnable dinleyici) {
        this.degisiklikDinleyici = dinleyici;
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(activity).inflate(R.layout.item_post_feed, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (position < 0 || position >= postList.size()) return;
        if (payloads.contains(PAYLOAD_SAYAC)) {
            sayaclariBagla(holder, postList.get(position));
        } else {
            onBindViewHolder(holder, position);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        Post post = postList.get(position);

        String ad = post.getYazarAd();
        if (ad == null || ad.isEmpty()) ad = activity.getString(R.string.bilinmeyen_kullanici);
        holder.textGonderenIsim.setText(ad);
        String kAdi = post.getYazarKullaniciAdi() == null ? "" : post.getYazarKullaniciAdi();
        holder.textGonderenKullaniciAdi.setText(kAdi);
        holder.textGonderenKullaniciAdi.setVisibility(kAdi.isEmpty() ? View.GONE : View.VISIBLE);
        holder.imageGonderenProfil.setImageTintList(null);
        ResimYukleyici.goster(holder.imageGonderenProfil, post.getYazarResimUrl());

        String metin = gonderiMetni(post);
        holder.textGonderiMetni.setText(metin);
        holder.textGonderiMetni.setVisibility(metin.isEmpty() ? View.GONE : View.VISIBLE);

        if (post.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault());
            holder.textGonderiTarihi.setText(sdf.format(new Date(post.getTimestamp())));
        } else {
            holder.textGonderiTarihi.setText(R.string.yeni);
        }

        if (post.getResimUri() != null && !post.getResimUri().isEmpty()) {
            holder.cardGonderiFoto.setVisibility(View.VISIBLE);
            holder.imageGonderiFoto.setImageTintList(null);
            ResimYukleyici.goster(holder.imageGonderiFoto, post.getResimUri());
        } else {
            holder.imageGonderiFoto.setImageDrawable(null);
            holder.cardGonderiFoto.setVisibility(View.GONE);
        }

        sayaclariBagla(holder, post);

        View.OnClickListener profileGit = v -> {
            Post p = konumdakiGonderi(holder);
            if (p != null && profilAcilabilir && p.getYazarUid() != null) {
                KullaniciProfilActivity.baslat(activity, p.getYazarUid());
            }
        };
        holder.layoutGonderenBilgi.setOnClickListener(profileGit);
        holder.cardGonderenProfil.setOnClickListener(profileGit);
        holder.btnBegen.setOnClickListener(v -> {
            Post p = konumdakiGonderi(holder);
            if (p != null) begeniDegistir(p);
        });
        holder.btnYorum.setOnClickListener(v -> {
            Post p = konumdakiGonderi(holder);
            if (p != null) yorumlariAc(p);
        });
        holder.iconSecenekler.setOnClickListener(v -> {
            Post p = konumdakiGonderi(holder);
            if (p != null) seceneklerMenusu(v, p);
        });
    }

    private void sayaclariBagla(FeedViewHolder holder, Post post) {
        boolean begenildi = post.getBulutId() != null && begenilenler.contains(post.getBulutId());
        holder.textBegenSayisi.setText(String.valueOf(post.getBegeniSayisi()));
        holder.textYorumSayisi.setText(String.valueOf(post.getYorumSayisi()));
        holder.iconBegen.setImageResource(begenildi ? android.R.drawable.star_on : android.R.drawable.star_off);
        int renk = begenildi
                ? Color.parseColor("#FFC107")
                : ContextCompat.getColor(activity, R.color.renk_metin_ikincil);
        holder.iconBegen.setImageTintList(ColorStateList.valueOf(renk));
        holder.textBegenSayisi.setTextColor(renk);
    }

    private Post konumdakiGonderi(FeedViewHolder holder) {
        int pos = holder.getAdapterPosition();
        return pos != RecyclerView.NO_POSITION && pos < postList.size() ? postList.get(pos) : null;
    }

    private int konumBul(String postId) {
        for (int i = 0; i < postList.size(); i++) {
            if (postId != null && postId.equals(postList.get(i).getBulutId())) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    private static String gonderiMetni(Post post) {
        String metin = post.getIcerik() != null && !post.getIcerik().trim().isEmpty() ? post.getIcerik() : post.getBaslik();
        return metin != null ? metin.trim() : "";
    }

    private boolean ekranGecerli() {
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private void begeniDegistir(Post post) {
        if (aktifUid.isEmpty() || post.getBulutId() == null) return;
        final String postId = post.getBulutId();
        final boolean begen = !begenilenler.contains(postId);

        if (begen) begenilenler.add(postId); else begenilenler.remove(postId);
        post.setBegeniSayisi(Math.max(0, post.getBegeniSayisi() + (begen ? 1 : -1)));
        sayacYenile(postId);

        BulutSosyal.begen(postId, aktifUid, begen, (sayi, hata) -> {
            if (!ekranGecerli()) return;
            if (hata != null || sayi == null) {
                if (begen) begenilenler.remove(postId); else begenilenler.add(postId);
                int i = konumBul(postId);
                if (i != RecyclerView.NO_POSITION) {
                    Post p = postList.get(i);
                    p.setBegeniSayisi(Math.max(0, p.getBegeniSayisi() + (begen ? -1 : 1)));
                    notifyItemChanged(i, PAYLOAD_SAYAC);
                }
                Toast.makeText(activity, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            int i = konumBul(postId);
            if (i == RecyclerView.NO_POSITION) return;
            postList.get(i).setBegeniSayisi(sayi);
            notifyItemChanged(i, PAYLOAD_SAYAC);
            if (begen && post.getYazarUid() != null && !aktifUid.equals(post.getYazarUid())) {
                BulutSosyal.bildirimBirak(post.getYazarUid(), com.mehmetokyay.flownote.util.BildirimMerkezi.TIP_BEGENI,
                        aktifUid, "", "", post.getBulutId());
            }
        });
    }

    private void sayacYenile(String postId) {
        int i = konumBul(postId);
        if (i != RecyclerView.NO_POSITION) notifyItemChanged(i, PAYLOAD_SAYAC);
    }

    private void yorumlariAc(Post post) {
        if (aktifUid.isEmpty() || post.getBulutId() == null) return;
        FragmentManager fm = activity.getSupportFragmentManager();
        if (fm.isStateSaved()) return;
        YorumlarBottomSheet sheet = YorumlarBottomSheet.yeni(post.getBulutId(), aktifUid);
        final String postId = post.getBulutId();
        sheet.setYorumSayisiDinleyici(sayi -> {
            int i = konumBul(postId);
            if (i == RecyclerView.NO_POSITION) return;
            postList.get(i).setYorumSayisi(sayi);
            notifyItemChanged(i, PAYLOAD_SAYAC);
        });
        sheet.show(fm, YorumlarBottomSheet.ETIKET);
    }

    private void seceneklerMenusu(View anchor, Post post) {
        boolean kendiGonderim = aktifUid.equals(post.getYazarUid());
        PopupMenu popup = new PopupMenu(activity, anchor);
        Menu menu = popup.getMenu();
        if (!kendiGonderim) {
            if (profilAcilabilir) menu.add(Menu.NONE, MENU_PROFIL, 0, R.string.profili_gor);
            menu.add(Menu.NONE, MENU_MESAJ, 1, R.string.mesaj_gonder);
        }
        menu.add(Menu.NONE, MENU_PAYLAS, 2, R.string.paylas);
        if (!gonderiMetni(post).isEmpty()) menu.add(Menu.NONE, MENU_KOPYALA, 3, R.string.metni_kopyala);
        if (kendiGonderim) {
            menu.add(Menu.NONE, MENU_SIL, 4, R.string.gonderiyi_sil);
        } else {
            menu.add(Menu.NONE, MENU_SIKAYET, 4, R.string.gonderiyi_sikayet_et);
            menu.add(Menu.NONE, MENU_ENGELLE, 5, R.string.kullaniciyi_engelle);
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == MENU_PROFIL) {
                KullaniciProfilActivity.baslat(activity, post.getYazarUid());
            } else if (id == MENU_MESAJ) {
                SohbetActivity.baslat(activity, post.getYazarUid());
            } else if (id == MENU_PAYLAS) {
                gonderiyiPaylas(post);
            } else if (id == MENU_KOPYALA) {
                metniKopyala(post);
            } else if (id == MENU_SIL) {
                silmeOnayi(post);
            } else if (id == MENU_ENGELLE) {
                engellemeOnayi(post);
            } else if (id == MENU_SIKAYET) {
                sikayetDiyalogu(post);
            } else {
                return false;
            }
            return true;
        });
        popup.show();
    }

    private void gonderiyiPaylas(Post post) {
        String ad = post.getYazarAd() != null && !post.getYazarAd().isEmpty()
                ? post.getYazarAd() : activity.getString(R.string.bilinmeyen_kullanici);
        String metin = activity.getString(R.string.gonderi_paylas_metin, ad, gonderiMetni(post));
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, metin);
        activity.startActivity(Intent.createChooser(intent, activity.getString(R.string.paylas)));
    }

    private void metniKopyala(Post post) {
        ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) return;
        cm.setPrimaryClip(ClipData.newPlainText("FlowNote", gonderiMetni(post)));
        Toast.makeText(activity, R.string.metin_kopyalandi, Toast.LENGTH_SHORT).show();
    }

    private void silmeOnayi(Post post) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.gonderiyi_sil)
                .setMessage(R.string.gonderiyi_sil_onay)
                .setPositiveButton(R.string.task_delete, (d, w) -> gonderiyiSil(post.getBulutId()))
                .setNegativeButton(R.string.iptal, null)
                .show();
    }

    private void gonderiyiSil(String postId) {
        BulutSosyal.gonderiSil(postId, aktifUid, (tamam, hata) -> {
            if (!ekranGecerli()) return;
            if (tamam == null || !tamam) {
                Toast.makeText(activity, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            int i = konumBul(postId);
            if (i != RecyclerView.NO_POSITION) {
                postList.remove(i);
                notifyItemRemoved(i);
            }
            Toast.makeText(activity, R.string.gonderi_silindi, Toast.LENGTH_SHORT).show();
            if (degisiklikDinleyici != null) degisiklikDinleyici.run();
        });
    }

    private void sikayetDiyalogu(Post post) {
        final int[] secilen = {-1};
        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.sikayet_baslik)
                .setSingleChoiceItems(R.array.sikayet_sebepleri, -1, (d, which) -> {
                    secilen[0] = which;
                    ((AlertDialog) d).getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                })
                .setPositiveButton(R.string.sikayet_et, (d, w) -> {
                    if (secilen[0] >= 0) sikayetGonder(post, secilen[0]);
                })
                .setNegativeButton(R.string.iptal, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private void sikayetGonder(Post post, int sebepIndeksi) {
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fUser == null || aktifUid.isEmpty()) return;
        final Context appContext = activity.getApplicationContext();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            User ben = null;
            try {
                ben = YerelKullanici.garantiEt(appContext, fUser);
            } catch (Exception e) {
                Log.e(TAG, "Şikayet eden okunamadı", e);
            }
            String ad = ben == null ? "" : (ben.getKullaniciAdi() == null ? "" : ben.getKullaniciAdi());
            BulutSosyal.sikayetEt(post, fUser.getUid(), ad, sebepIndeksi, (basarili, hata) -> {
                if (!ekranGecerli()) return;
                Toast.makeText(activity, Boolean.TRUE.equals(basarili) ? R.string.sikayet_alindi : R.string.sikayet_gonderilemedi,
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    private void engellemeOnayi(Post post) {
        String ad = post.getYazarAd() != null && !post.getYazarAd().isEmpty()
                ? post.getYazarAd() : activity.getString(R.string.bilinmeyen_kullanici);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.kullaniciyi_engelle)
                .setMessage(activity.getString(R.string.engelle_onay, ad))
                .setPositiveButton(R.string.engelle, (d, w) -> kullaniciyiEngelle(post.getYazarUid(), ad))
                .setNegativeButton(R.string.iptal, null)
                .show();
    }

    private void kullaniciyiEngelle(String hedefUid, String ad) {
        BulutSosyal.engelle(aktifUid, hedefUid, (tamam, hata) -> {
            if (!ekranGecerli()) return;
            if (tamam == null || !tamam) {
                Toast.makeText(activity, R.string.hata_tekrar_deneyin, Toast.LENGTH_SHORT).show();
                return;
            }
            for (int i = postList.size() - 1; i >= 0; i--) {
                if (hedefUid.equals(postList.get(i).getYazarUid())) {
                    postList.remove(i);
                    notifyItemRemoved(i);
                }
            }
            Toast.makeText(activity, activity.getString(R.string.kullanici_engellendi, ad), Toast.LENGTH_SHORT).show();
            if (degisiklikDinleyici != null) degisiklikDinleyici.run();
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class FeedViewHolder extends RecyclerView.ViewHolder {
        TextView textGonderenIsim, textGonderenKullaniciAdi, textGonderiTarihi, textGonderiMetni, textBegenSayisi, textYorumSayisi;
        ImageView imageGonderenProfil, iconSecenekler, iconBegen, imageGonderiFoto;
        LinearLayout layoutGonderenBilgi, btnBegen, btnYorum;
        View cardGonderenProfil, cardGonderiFoto;

        public FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            textGonderenIsim = itemView.findViewById(R.id.textGonderenIsim);
            textGonderenKullaniciAdi = itemView.findViewById(R.id.textGonderenKullaniciAdi);
            textGonderiTarihi = itemView.findViewById(R.id.textGonderiTarihi);
            textGonderiMetni = itemView.findViewById(R.id.textGonderiMetni);
            textBegenSayisi = itemView.findViewById(R.id.textBegenSayisi);
            textYorumSayisi = itemView.findViewById(R.id.textYorumSayisi);
            imageGonderenProfil = itemView.findViewById(R.id.imageGonderenProfil);
            iconSecenekler = itemView.findViewById(R.id.iconSecenekler);
            iconBegen = itemView.findViewById(R.id.iconBegen);
            layoutGonderenBilgi = itemView.findViewById(R.id.layoutGonderenBilgi);
            btnBegen = itemView.findViewById(R.id.btnBegen);
            btnYorum = itemView.findViewById(R.id.btnYorum);
            cardGonderenProfil = itemView.findViewById(R.id.cardGonderenProfil);
            cardGonderiFoto = itemView.findViewById(R.id.cardGonderiFoto);
            imageGonderiFoto = itemView.findViewById(R.id.imageGonderiFoto);
        }
    }
}
