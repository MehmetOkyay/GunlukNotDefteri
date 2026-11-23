package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.Blob;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class ResimYukleyici {

    public static final String ONEK = "fnimg:";
    private static final int AZAMI_BAYT = 650_000;
    private static final android.util.LruCache<String, byte[]> ONBELLEK = new android.util.LruCache<String, byte[]>(4 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, byte[] value) {
            return value == null ? 0 : value.length;
        }
    };

    public interface UrlSonuc {
        void bitti(@Nullable String url, @Nullable Exception hata);
    }

    private static final Handler ANA_THREAD = new Handler(Looper.getMainLooper());

    private ResimYukleyici() {
    }

    private static void anaThreadde(UrlSonuc sonuc, @Nullable String url, @Nullable Exception hata) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            sonuc.bitti(url, hata);
        } else {
            ANA_THREAD.post(() -> sonuc.bitti(url, hata));
        }
    }

    public static void goster(ImageView iv, @Nullable String yolVeyaUrl) {
        if (iv == null) return;
        try {
            iv.setTag(yolVeyaUrl);
            if (yolVeyaUrl == null || yolVeyaUrl.isEmpty()) {
                Glide.with(iv).clear(iv);
                iv.setImageResource(android.R.drawable.ic_menu_camera);
                return;
            }
            if (yolVeyaUrl.startsWith(ONEK)) {
                byte[] hazir = ONBELLEK.get(yolVeyaUrl);
                if (hazir != null) {
                    Glide.with(iv).load(hazir).diskCacheStrategy(DiskCacheStrategy.NONE)
                            .centerCrop().into(iv);
                    return;
                }
                iv.setImageResource(android.R.drawable.ic_menu_camera);
                String belge = yolVeyaUrl.substring(ONEK.length());
                FirebaseFirestore.getInstance().collection("medya").document(belge).get()
                        .addOnSuccessListener(d -> {
                            if (!yolVeyaUrl.equals(iv.getTag()) || d == null || d.getBlob("veri") == null) return;
                            byte[] veri = d.getBlob("veri").toBytes();
                            ONBELLEK.put(yolVeyaUrl, veri);
                            try {
                                Glide.with(iv).load(veri).centerCrop().into(iv);
                            } catch (RuntimeException ignored) {
                            }
                        });
                return;
            }
            Object kaynak = yolVeyaUrl.startsWith("http") || yolVeyaUrl.startsWith("content:")
                    ? yolVeyaUrl
                    : new File(yolVeyaUrl);
            Glide.with(iv)
                    .load(kaynak)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .centerCrop()
                    .into(iv);
        } catch (RuntimeException ignored) {
        }
    }

    public static void yukle(Context context, Uri uri, String depolamaYolu, UrlSonuc sonuc) {
        byte[] jpeg;
        try {
            jpeg = jpegOku(context, uri);
        } catch (Exception e) {
            anaThreadde(sonuc, null, e);
            return;
        } catch (OutOfMemoryError oom) {
            anaThreadde(sonuc, null, new IllegalStateException("resim çok büyük", oom));
            return;
        }
        if (jpeg == null || jpeg.length > 700_000) {
            anaThreadde(sonuc, null, new IllegalStateException("resim okunamadı"));
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            anaThreadde(sonuc, null, new IllegalStateException("oturum yok"));
            return;
        }
        String belge = depolamaYolu.replace('/', '_');
        Map<String, Object> veri = new HashMap<>();
        veri.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        veri.put("veri", Blob.fromBytes(jpeg));
        FirebaseFirestore.getInstance().collection("medya").document(belge).set(veri)
                .addOnSuccessListener(v -> {
                    ONBELLEK.put(ONEK + belge, jpeg);
                    sonuc.bitti(ONEK + belge, null);
                })
                .addOnFailureListener(e -> sonuc.bitti(null, e));
    }

    private static byte[] jpegOku(Context context, Uri uri) throws Exception {
        BitmapFactory.Options sinir = new BitmapFactory.Options();
        sinir.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, sinir);
        }
        byte[] jpeg = sikistir(context, uri, sinir, 1280, 78);
        int kenar = 1280;
        int kalite = 78;
        while (jpeg != null && jpeg.length > AZAMI_BAYT && kenar > 480) {
            kenar = kenar * 3 / 4;
            kalite = Math.max(52, kalite - 8);
            jpeg = sikistir(context, uri, sinir, kenar, kalite);
        }
        return jpeg;
    }

    private static byte[] sikistir(Context context, Uri uri, BitmapFactory.Options sinir,
                                   int maxKenar, int kalite) throws Exception {
        int ornek = 1;
        int kenar = Math.max(sinir.outWidth, sinir.outHeight);
        while (kenar / ornek > maxKenar) ornek *= 2;

        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inSampleSize = Math.max(1, ornek);
        Bitmap bmp;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, o);
        }
        if (bmp == null) return null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, kalite, bos);
        bmp.recycle();
        return bos.toByteArray();
    }
}
