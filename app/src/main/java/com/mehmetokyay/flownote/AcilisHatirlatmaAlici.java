package com.mehmetokyay.flownote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mehmetokyay.flownote.util.GorevHatirlatma;

public class AcilisHatirlatmaAlici extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String eylem = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(eylem)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(eylem)) {
            GorevHatirlatma.tumunuPlanla(context);
        }
    }
}
