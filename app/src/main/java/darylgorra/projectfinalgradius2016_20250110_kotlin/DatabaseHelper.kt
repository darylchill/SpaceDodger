package darylgorra.projectfinalgradius2016_20250110_kotlin

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    companion object {
        private const val DATABASE_NAME = "players.db"
        private const val TABLE_NAME = "tbl_players"
        private val COL1 = "ID"
        private const val COL2 = "NAME"
        private const val COL3 = "TIME"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE table $TABLE_NAME (ID INTEGER PRIMARY KEY AUTOINCREMENT, NAME TEXT, TIME TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(String.format("DROP TABLE IF EXISTS%s", TABLE_NAME))
        onCreate(db)
    }

    fun insertData(name: String, time: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(COL2, name)
        contentValues.put(COL3, time)
        val checkInsert = db.insert(TABLE_NAME, null, contentValues)
        return checkInsert != -1L
    }

    fun getAllData(): Cursor {
        val db = this.writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }
}
