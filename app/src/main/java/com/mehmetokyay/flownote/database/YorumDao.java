package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.mehmetokyay.flownote.model.Yorum;

import java.util.List;

@Dao
public interface YorumDao {

    @Insert
    long yorumEkle(Yorum yorum);

    @Query("SELECT * FROM yorum_table WHERE postId = :postId ORDER BY timestamp ASC, id ASC")
    List<Yorum> gonderiYorumlariniGetir(int postId);

    @Query("SELECT * FROM yorum_table WHERE id = :id LIMIT 1")
    Yorum idIleGetir(int id);

    @Query("DELETE FROM yorum_table WHERE id = :id")
    void yorumSil(int id);

    @Query("DELETE FROM yorum_table WHERE postId = :postId")
    void gonderiYorumlariniSil(int postId);
}
