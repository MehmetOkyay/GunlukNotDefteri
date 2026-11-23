package com.mehmetokyay.flownote.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public final class FotoDenetimi {

    private static final String TAG = "FotoDenetimi";
    private static final String MODEL = "nsfw.tflite";
    private static final int GIRDI = 224;
    private static final float ESIK = 0.58f;
    private static final float B_ORT = 104f;
    private static final float G_ORT = 117f;
    private static final float R_ORT = 123f;

    private static final Object KILIT = new Object();
    private static Interpreter interpreter;

    public enum Sonuc {
        UYGUN,
        UYGUNSUZ,
        OKUNAMADI
    }

    private FotoDenetimi() {
    }

    public static Sonuc denetle(Context context, @Nullable Uri uri) {
        if (uri == null) return Sonuc.UYGUN;
        Bitmap ham = bitmapYukle(context, uri);
        if (ham == null) return Sonuc.OKUNAMADI;
        Bitmap kare = null;
        try {
            kare = kareyeSigdir(ham, GIRDI);
            float skor = nsfwSkoru(context.getApplicationContext(), kare);
            if (skor < 0) return Sonuc.OKUNAMADI;
            Log.d(TAG, "NSFW skoru: " + skor);
            return skor >= ESIK ? Sonuc.UYGUNSUZ : Sonuc.UYGUN;
        } catch (Exception | LinkageError | OutOfMemoryError e) {
            synchronized (KILIT) {
                if (interpreter != null) {
                    try {
                        interpreter.close();
                    } catch (RuntimeException ignored) {
                    }
                    interpreter = null;
                }
            }
            Log.e(TAG, "Fotoğraf denetlenemedi", e);
            return Sonuc.OKUNAMADI;
        } finally {
            if (kare != null && kare != ham) kare.recycle();
            ham.recycle();
        }
    }

    private static float nsfwSkoru(Context context, Bitmap kare) throws IOException {
        synchronized (KILIT) {
            Interpreter tf = interpreterAl(context);
            Tensor girdi = tf.getInputTensor(0);
            Tensor cikti = tf.getOutputTensor(0);
            int[] girdiSekil = girdi.shape();
            int yukseklik = girdiSekil.length >= 3 ? girdiSekil[1] : GIRDI;
            int genislik = girdiSekil.length >= 3 ? girdiSekil[2] : GIRDI;
            Bitmap kullan = (kare.getWidth() == genislik && kare.getHeight() == yukseklik)
                    ? kare : Bitmap.createScaledBitmap(kare, genislik, yukseklik, true);

            Object girdiNesne;
            if (girdi.dataType() == DataType.FLOAT32) {
                ByteBuffer buf = ByteBuffer.allocateDirect(4 * genislik * yukseklik * 3).order(ByteOrder.nativeOrder());
                pikselleriYaz(kullan, buf, genislik, yukseklik);
                girdiNesne = buf;
            } else {
                ByteBuffer buf = ByteBuffer.allocateDirect(genislik * yukseklik * 3).order(ByteOrder.nativeOrder());
                pikselleriYazUint8(kullan, buf, genislik, yukseklik);
                girdiNesne = buf;
            }
            if (kullan != kare) kullan.recycle();

            int[] ciktiSekil = cikti.shape();
            int cikisAdet = 1;
            for (int d : ciktiSekil) cikisAdet *= d;
            if (cikti.dataType() == DataType.FLOAT32 && ciktiSekil.length == 2) {
                float[][] cikis2 = new float[1][Math.max(cikisAdet, 2)];
                tf.run(girdiNesne, cikis2);
                return skorSec(cikis2[0]);
            }
            Map<Integer, Object> ciktilar = new HashMap<>();
            ByteBuffer cikisBuf = ByteBuffer.allocateDirect(4 * Math.max(cikisAdet, 1)).order(ByteOrder.nativeOrder());
            ciktilar.put(0, cikisBuf);
            tf.runForMultipleInputsOutputs(new Object[]{girdiNesne}, ciktilar);
            cikisBuf.rewind();
            float[] duz = new float[cikisAdet];
            cikisBuf.asFloatBuffer().get(duz);
            return skorSec(duz);
        }
    }

    private static float skorSec(float[] cikis) {
        if (cikis.length >= 5) {
            return Math.max(cikis[1], Math.max(cikis[3], cikis[4] * 0.85f));
        }
        if (cikis.length >= 2) return cikis[1];
        return cikis.length == 1 ? cikis[0] : 0f;
    }

    private static void pikselleriYaz(Bitmap bmp, ByteBuffer buf, int w, int h) {
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        for (int p : px) {
            buf.putFloat((p & 0xFF) - B_ORT);
            buf.putFloat(((p >> 8) & 0xFF) - G_ORT);
            buf.putFloat(((p >> 16) & 0xFF) - R_ORT);
        }
    }

    private static void pikselleriYazUint8(Bitmap bmp, ByteBuffer buf, int w, int h) {
        int[] px = new int[w * h];
        bmp.getPixels(px, 0, w, 0, 0, w, h);
        for (int p : px) {
            buf.put((byte) ((p >> 16) & 0xFF));
            buf.put((byte) ((p >> 8) & 0xFF));
            buf.put((byte) (p & 0xFF));
        }
    }

    private static Interpreter interpreterAl(Context context) throws IOException {
        if (interpreter != null) return interpreter;
        Interpreter.Options opt = new Interpreter.Options();
        opt.setNumThreads(1);
        interpreter = new Interpreter(modelYukle(context), opt);
        return interpreter;
    }

    private static MappedByteBuffer modelYukle(Context context) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(MODEL);
        try (FileInputStream fis = new FileInputStream(fd.getFileDescriptor())) {
            FileChannel kanal = fis.getChannel();
            return kanal.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        } finally {
            fd.close();
        }
    }

    private static Bitmap bitmapYukle(Context context, Uri uri) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        o.inSampleSize = ornekBoyu(context, uri);
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) return null;
            return BitmapFactory.decodeStream(in, null, o);
        } catch (Exception | OutOfMemoryError e) {
            Log.e(TAG, "Bitmap okunamadı", e);
            return null;
        }
    }

    private static int ornekBoyu(Context context, Uri uri) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        } catch (Exception e) {
            return 1;
        }
        int maxKenar = Math.max(o.outWidth, o.outHeight);
        int ornek = 1;
        while (maxKenar / ornek > 1024) ornek *= 2;
        return ornek;
    }

    private static Bitmap kareyeSigdir(Bitmap kaynak, int kenar) {
        int w = kaynak.getWidth();
        int h = kaynak.getHeight();
        if (w <= 0 || h <= 0) return Bitmap.createScaledBitmap(kaynak, kenar, kenar, true);
        int kisa = Math.min(w, h);
        Bitmap kirp = Bitmap.createBitmap(kaynak, (w - kisa) / 2, (h - kisa) / 2, kisa, kisa);
        if (kirp.getWidth() == kenar && kirp.getHeight() == kenar) return kirp;
        Bitmap olcek = Bitmap.createScaledBitmap(kirp, kenar, kenar, true);
        if (kirp != kaynak) kirp.recycle();
        return olcek;
    }
}
