package com.mehmetokyay.flownote.util;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.mehmetokyay.flownote.R;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class IcerikDenetimi {

    public enum Kategori {
        NEFRET(R.string.icerik_red_nefret),
        SIDDET(R.string.icerik_red_siddet),
        CINSEL(R.string.icerik_red_cinsel);

        @StringRes
        public final int mesaj;

        Kategori(@StringRes int mesaj) {
            this.mesaj = mesaj;
        }
    }

    private static final Locale TR = new Locale("tr", "TR");
    private static final Pattern AYIRICI = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Pattern TEKRAR = Pattern.compile("(.)\\1{2,}");

    private static final Set<String> NEFRET_KELIME = setOf(
            "nazi", "naziler", "heilhitler", "whitepower", "kkk",
            "nigger", "nigga", "niggas", "negro", "kike", "spic", "chink", "paki", "wetback",
            "faggot", "fag", "tranny", "retard", "retarded",
            "yahudilerinsiksin", "muslumanlarinsiksin",
            "cingene", "cingeneherif", "zenci", "siyahilere"
    );
    private static final String[] NEFRET_IFADE = {
            "heil hitler", "gas the", "kill all", "death to all",
            "all jews", "all muslims", "all blacks", "all gays",
            "irkci", "irkcilik", "irkciyim",
            "soykirim yap", "soykirim yapilmali",
            "hepiniz olun", "hepiniz geberin", "hepinizi oldurecegim",
            "suriyeliler defolsun", "multeciler defolsun", "araplar defolsun",
            "kurtler defolsun", "ermeni belasi", "yahudi belasi"
    };

    private static final Set<String> SIDDET_KELIME = setOf(
            "oldurecegim", "oldurecem", "oldurcem", "oldurucem", "gebertecegim", "gebertecem",
            "infaz", "katliam", "katliamini", "bombala", "bombalayacagim", "patlatacagim",
            "tecavuz", "tecavuzet", "tecavuzedecegim", "iskence", "iskenceyap",
            "behead", "decapitate", "schoolshooting"
    );
    private static final String[] SIDDET_IFADE = {
            "seni oldurecegim", "seni oldururum", "kafani keserim", "kafasini kes",
            "kanini icerim", "canina okuyacagim", "evini basarim",
            "silahla vur", "silahla vuracagim", "bicakla oldur",
            "i will kill", "i'll kill you", "rape you", "blow up the"
    };

    private static final Set<String> CINSEL_KELIME = setOf(
            "amcik", "amcigi", "yarrak", "yarak", "sikis", "sikiş", "sikiyorum", "sikecegim",
            "sikeyim", "siktir", "siktirgit", "orospu", "orospucocugu", "pezevenk",
            "porno", "porn", "pornografi", "hentai", "onlyfans",
            "penis", "vajina", "vagina", "klitoris", "testis",
            "masturbasyon", "masturbation", "orgazm"
    );
    private static final String[] CINSEL_IFADE = {
            "cinsel organ", "cinsel organim", "tassak", "taşşak",
            "nude pic", "nudes", "send nudes", "dick pic", "sex tape"
    };

    private static final Set<String> KISA_CINSEL = setOf("got", "gotu", "gotune", "amina", "amini", "aminiyim");

    private IcerikDenetimi() {
    }

    @Nullable
    public static Kategori metniDenetle(@Nullable String ham) {
        if (ham == null || ham.trim().isEmpty()) return null;
        String duz = normallestir(ham);
        String bitisik = AYIRICI.matcher(duz).replaceAll("");
        Set<String> kelimeler = new HashSet<>(Arrays.asList(AYIRICI.split(duz)));

        if (ifadeVar(duz, bitisik, NEFRET_IFADE) || kelimeVar(kelimeler, bitisik, NEFRET_KELIME, 6)) {
            return Kategori.NEFRET;
        }
        if (ifadeVar(duz, bitisik, SIDDET_IFADE) || kelimeVar(kelimeler, bitisik, SIDDET_KELIME, 6)) {
            return Kategori.SIDDET;
        }
        if (ifadeVar(duz, bitisik, CINSEL_IFADE)
                || kelimeVar(kelimeler, bitisik, CINSEL_KELIME, 5)
                || kesisir(kelimeler, KISA_CINSEL)) {
            return Kategori.CINSEL;
        }
        return null;
    }

    private static boolean ifadeVar(String duz, String bitisik, String[] ifadeler) {
        for (String ifade : ifadeler) {
            String n = normallestir(ifade);
            if (n.isEmpty()) continue;
            if (duz.contains(n)) return true;
            String b = AYIRICI.matcher(n).replaceAll("");
            if (b.length() >= 8 && bitisik.contains(b)) return true;
        }
        return false;
    }

    private static boolean kelimeVar(Set<String> kelimeler, String bitisik, Set<String> yasak, int bitisikMin) {
        if (kesisir(kelimeler, yasak)) return true;
        for (String y : yasak) {
            if (y.length() >= bitisikMin && bitisik.contains(y)) return true;
        }
        return false;
    }

    private static boolean kesisir(Set<String> a, Set<String> b) {
        for (String x : a) {
            if (b.contains(x)) return true;
        }
        return false;
    }

    static String normallestir(String ham) {
        String s = ham.toLowerCase(TR);
        s = s.replace('ı', 'i').replace('İ', 'i')
                .replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o')
                .replace('ç', 'c');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replace('0', 'o').replace('1', 'i').replace('3', 'e')
                .replace('4', 'a').replace('5', 's').replace('7', 't')
                .replace('@', 'a').replace('$', 's');
        s = TEKRAR.matcher(s).replaceAll("$1$1");
        return s.trim();
    }

    private static Set<String> setOf(String... items) {
        return new HashSet<>(Arrays.asList(items));
    }
}
