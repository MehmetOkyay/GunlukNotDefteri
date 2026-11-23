package com.mehmetokyay.flownote.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.mehmetokyay.flownote.model.Begeni;

import java.util.List;

@Dao
public interface BegeniDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long begeniEkle(Begeni begeni);

    @Query("DELETE FROM begeni_table WHERE postId = :postId AND userId = :userId")
    int begeniSil(int postId, int userId);

    @Query("SELECT postId FROM begeni_table WHERE userId = :userId")
    List<Integer> begenilenPostIdleri(int userId);

    @Query("DELETE FROM begeni_table WHERE postId = :postId")
    void gonderiBegenileriniSil(int postId);
}
