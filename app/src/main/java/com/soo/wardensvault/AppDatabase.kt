package com.soo.wardensvault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//database class that holds the database and serves as main access point

/*
class must contain @Database annotation and an entities array that
contains all data entities associated with the database
 */
@Database(entities = [User::class], version = 1)
//abstract class because it extends RoomDatabase
abstract class AppDatabase : RoomDatabase() {
    /*
    must define an abstract method with no arguments that returns
    DAO class. Must be created for each DAO class
     */

    abstract fun userDao(): UserDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        //creates instance of database
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}