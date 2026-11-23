package com.mehmetokyay.flownote.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "Users")
public class User {
    @PrimaryKey(autoGenerate = true)
    private int userId;

    @ColumnInfo(name = "isim")
    private String userIsim;

    @ColumnInfo(name = "soyisim")
    private String userSoyisim;

    @ColumnInfo(name = "dogum_tarihi")
    private String dogumTarihi;

    @ColumnInfo(name = "email")
    private String email;

    @ColumnInfo(name = "sifre")
    private String sifre;

    @ColumnInfo(name = "kullanici_adi")
    private String kullaniciAdi;

    @ColumnInfo(name = "meslek")
    private String meslek;

    @ColumnInfo(name = "konum")
    private String konum;

    @ColumnInfo(name = "web_sitesi")
    private String webSitesi;

    @ColumnInfo(name = "biyografi")
    private String biyografi;

    @ColumnInfo(name = "profil_resmi")
    private String profilResmi;

    @ColumnInfo(name = "takipci_sayisi")
    private int takipciSayisi = 0;

    @ColumnInfo(name = "takip_edilen_sayisi")
    private int takipEdilenSayisi = 0;

    @ColumnInfo(name = "engellenen_kullanicilar")
    private String engellenenKullanicilar = "";

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserIsim() { return userIsim; }
    public void setUserIsim(String userIsim) { this.userIsim = userIsim; }

    public String getUserSoyisim() { return userSoyisim; }
    public void setUserSoyisim(String userSoyisim) { this.userSoyisim = userSoyisim; }

    public String getDogumTarihi() { return dogumTarihi; }
    public void setDogumTarihi(String dogumTarihi) { this.dogumTarihi = dogumTarihi; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSifre() { return sifre; }
    public void setSifre(String sifre) { this.sifre = sifre; }

    public String getKullaniciAdi() { return kullaniciAdi; }
    public void setKullaniciAdi(String kullaniciAdi) { this.kullaniciAdi = kullaniciAdi; }

    public String getMeslek() { return meslek; }
    public void setMeslek(String meslek) { this.meslek = meslek; }

    public String getKonum() { return konum; }
    public void setKonum(String konum) { this.konum = konum; }

    public String getWebSitesi() { return webSitesi; }
    public void setWebSitesi(String webSitesi) { this.webSitesi = webSitesi; }

    public String getBiyografi() { return biyografi; }
    public void setBiyografi(String biyografi) { this.biyografi = biyografi; }

    public String getProfilResmi() { return profilResmi; }
    public void setProfilResmi(String profilResmi) { this.profilResmi = profilResmi; }

    public int getTakipciSayisi() { return takipciSayisi; }
    public void setTakipciSayisi(int takipciSayisi) { this.takipciSayisi = takipciSayisi; }

    public int getTakipEdilenSayisi() { return takipEdilenSayisi; }
    public void setTakipEdilenSayisi(int takipEdilenSayisi) { this.takipEdilenSayisi = takipEdilenSayisi; }

    public String getEngellenenKullanicilar() { return engellenenKullanicilar; }
    public void setEngellenenKullanicilar(String engellenenKullanicilar) { this.engellenenKullanicilar = engellenenKullanicilar; }
}