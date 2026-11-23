package com.mehmetokyay.flownote.util;

import androidx.annotation.StringRes;

import com.mehmetokyay.flownote.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public final class AuthHataYardimcisi {

    private AuthHataYardimcisi() {
    }

    @StringRes
    public static int girisHatasi(Exception e) {
        if (e instanceof FirebaseNetworkException) return R.string.auth_ag_hatasi;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.auth_cok_deneme;
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return R.string.login_basarisiz;
        }
        return R.string.auth_genel_hata;
    }

    @StringRes
    public static int kayitHatasi(Exception e) {
        if (e instanceof FirebaseNetworkException) return R.string.auth_ag_hatasi;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.auth_cok_deneme;
        if (e instanceof FirebaseAuthUserCollisionException) return R.string.register_eposta_kayitli;
        if (e instanceof FirebaseAuthInvalidCredentialsException) return R.string.register_gecersiz_eposta;
        return R.string.register_hata;
    }

    @StringRes
    public static int genelHata(Exception e) {
        if (e instanceof FirebaseNetworkException) return R.string.auth_ag_hatasi;
        if (e instanceof FirebaseTooManyRequestsException) return R.string.auth_cok_deneme;
        return R.string.auth_genel_hata;
    }
}
