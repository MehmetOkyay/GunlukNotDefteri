package com.mehmetokyay.flownote.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.mehmetokyay.flownote.model.Begeni;
import com.mehmetokyay.flownote.model.Mesaj;
import com.mehmetokyay.flownote.model.Post;
import com.mehmetokyay.flownote.model.Task;
import com.mehmetokyay.flownote.model.User;
import com.mehmetokyay.flownote.model.Yorum;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Task.class, User.class, Post.class, Begeni.class, Yorum.class, Mesaj.class},
        version = 5, exportSchema = false)
public abstract class TaskDatabase extends RoomDatabase {

    public abstract TaskDao taskDao();
    public abstract UserDao userDao();
    public abstract PostDao postDao();
    public abstract BegeniDao begeniDao();
    public abstract YorumDao yorumDao();
    public abstract MesajDao mesajDao();

    private static volatile TaskDatabase instance;

    public static final ExecutorService ARKA_PLAN = Executors.newFixedThreadPool(4);

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN hatirlat INTEGER NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `begeni_table` (`postId` INTEGER NOT NULL, "
                    + "`userId` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`postId`, `userId`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `yorum_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`postId` INTEGER NOT NULL, `userId` INTEGER NOT NULL, `metin` TEXT, `timestamp` INTEGER NOT NULL)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `mesaj_table` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`gonderenId` INTEGER NOT NULL, `aliciId` INTEGER NOT NULL, `metin` TEXT, "
                    + "`timestamp` INTEGER NOT NULL, `okundu` INTEGER NOT NULL)");
        }
    };

    public static TaskDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TaskDatabase.class) {
                if (instance == null) {
                    Context app = context.getApplicationContext();
                    RoomDatabase.Builder<TaskDatabase> builder = Room.databaseBuilder(
                            app, TaskDatabase.class, "task_database")
                            .addMigrations(MIGRATION_3_4, MIGRATION_4_5);
                    boolean debug = (app.getApplicationInfo().flags
                            & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
                    if (debug) {
                        builder.fallbackToDestructiveMigration();
                    }
                    instance = builder.build();
                }
            }
        }
        return instance;
    }
}
