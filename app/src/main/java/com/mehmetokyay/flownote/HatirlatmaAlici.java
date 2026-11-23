package com.mehmetokyay.flownote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mehmetokyay.flownote.database.TaskDatabase;
import com.mehmetokyay.flownote.model.Task;
import com.mehmetokyay.flownote.util.BildirimMerkezi;
import com.mehmetokyay.flownote.util.GorevHatirlatma;

public class HatirlatmaAlici extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        long gorevId = intent.getLongExtra(GorevHatirlatma.EKSTRA_GOREV_ID, -1);
        if (gorevId <= 0) return;
        Context app = context.getApplicationContext();
        PendingResult bekleyen = goAsync();
        TaskDatabase.ARKA_PLAN.execute(() -> {
            try {
                Task gorev = TaskDatabase.getInstance(app).taskDao().secileniGoster(gorevId);
                if (gorev != null && gorev.isHatirlat()) {
                    BildirimMerkezi.gorevBildirimiGoster(app, gorev);
                    GorevHatirlatma.planla(app, gorev);
                }
            } catch (Exception ignored) {
            } finally {
                bekleyen.finish();
            }
        });
    }
}
