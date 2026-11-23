package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.mehmetokyay.flownote.model.User;

import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void kisiEkle(User yeniUser);

    @Update
    void kisiGuncelle(User guncellenecekUser);

    @Delete
    void kisiSil(User silinecekUser);

    @Query("SELECT * FROM Users WHERE email = :email LIMIT 1")
    User emailIleKullaniciGetir(String email);

    @Update
    void kullaniciGuncelle(User user);

    @Query("SELECT COUNT(*) FROM Users WHERE kullanici_adi = :kullaniciAdi")
    int kullaniciAdiSayisi(String kullaniciAdi);

    @Query("SELECT * FROM Users WHERE userId = :id LIMIT 1")
    User idIleKullaniciGetir(int id);

    @Query("UPDATE Users SET engellenen_kullanicilar = :liste WHERE userId = :id")
    void engellenenleriGuncelle(int id, String liste);

    @Query("SELECT * FROM Users WHERE userId IN (:idler)")
    List<User> idlerIleGetir(List<Integer> idler);

    @Query("SELECT * FROM Users ORDER BY isim COLLATE NOCASE")
    List<User> tumKullanicilariGetir();

}