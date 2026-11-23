const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

exports.bildirimGonder = onDocumentCreated("kullanicilar/{uid}/bildirimler/{bid}", async (event) => {
  const uid = event.params.uid;
  const veri = event.data == null ? null : event.data.data();
  if (!veri) return;

  const cihaz = await getFirestore().doc(`kullanicilar/${uid}/cihaz/ana`).get();
  const token = cihaz.get("token");
  if (!token) return;

  try {
    await getMessaging().send({
      token,
      data: {
        tip: String(veri.tip || ""),
        gonderenAd: String(veri.gonderenAd || ""),
        metin: String(veri.metin || ""),
        gonderenUid: String(veri.gonderenUid || ""),
        ilgiliId: String(veri.ilgiliId || ""),
      },
      android: {priority: "high"},
    });
  } catch (hata) {
    const kod = hata && hata.code ? String(hata.code) : "";
    if (kod.includes("registration-token-not-registered") || kod.includes("invalid-argument")) {
      await cihaz.ref.delete();
    } else {
      console.error("FCM gönderilemedi", hata);
    }
  }
});
