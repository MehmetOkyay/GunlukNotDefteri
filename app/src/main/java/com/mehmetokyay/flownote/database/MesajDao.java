package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mehmetokyay.flownote.model.Mesaj;

import java.util.List;

@Dao
public interface MesajDao {

    @Insert
    long mesajEkle(Mesaj mesaj);

    @Query("SELECT * FROM mesaj_table WHERE (gonderenId = :a AND aliciId = :b) OR (gonderenId = :b AND aliciId = :a) ORDER BY timestamp ASC, id ASC")
    List<Mesaj> sohbetiGetir(int a, int b);

    @Query("SELECT * FROM mesaj_table WHERE gonderenId = :kullaniciId OR aliciId = :kullaniciId ORDER BY timestamp DESC, id DESC")
    List<Mesaj> kullanicininMesajlariniGetir(int kullaniciId);

    @Query("UPDATE mesaj_table SET okundu = 1 WHERE aliciId = :ben AND gonderenId = :karsi AND okundu = 0")
    void okunduIsaretle(int ben, int karsi);
}
