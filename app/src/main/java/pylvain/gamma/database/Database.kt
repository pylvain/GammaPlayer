package pylvain.gamma.library

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import org.koin.core.component.KoinComponent
import pylvain.gamma.*

@Entity(tableName = "libraries")
data class LibraryEntry(
    @PrimaryKey var source: String,
)

@Entity(
    tableName = "books",
    indices = [Index(value = ["librarySource"])],
    foreignKeys = [
        ForeignKey(
            entity = LibraryEntry::class,
            parentColumns = arrayOf("source"), childColumns = arrayOf("librarySource"),
            onDelete = CASCADE
        ),
    ]
)
data class BookEntry(
    @PrimaryKey var source: String,
    var librarySource: String,
    var playlist: List<String>, // -> Files
    var title: String,
    var author: String,
    var totalDuration: Long = -1,
    var dateAdded: Long,
    var fileSizeSum: Long,
    @Embedded(prefix = "settings_") var settings: BookSettings,
)

data class BookSettings(
    var cursor: Long = -1, // -> Bookmark
    var lastSought: Long = -1, // -> Bookmark
    var volume: Float = 1f,
    var loudness: Int = 0,
    var speed: Float = 1f,
    //  ar balance: Int = 0,
    var isNew: Boolean = true,
    var isCompleted: Boolean = false,
    //var mono: Boolean = false,
    var skipSilences: Boolean = false,
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = BookEntry::class,
        parentColumns = arrayOf("source"), childColumns = arrayOf("sourceBook"),
        onDelete = CASCADE
    )
    ]
)
data class BookmarkEntry(
    @PrimaryKey(autoGenerate = true) var id: Long = 0,
    var file: String, // -> File
    var sourceBook: String = "", // -> Book
    var time: Long = 0,
    var name: String = "",
    var description: String = "",
    var tags: String = "",
)

@Entity(
    tableName = "files",
    indices = [Index(value = ["sourceBook"])],
    foreignKeys = [
        ForeignKey(
            entity = BookEntry::class,
            parentColumns = arrayOf("source"), childColumns = arrayOf("sourceBook"),
            onDelete = CASCADE
        ),
    ]
)
data class FileEntry(
    @PrimaryKey var source: String,
    var sourceBook: String,
    var kind: String,
    var size: Long = 0, //serve as hash
    var duration: Long = 0,
)

class Converters {
    @TypeConverter
    fun stringOfLongList(l: List<Long>): String =
        l.fold("") { acc, it -> "$acc?$it" }

    @TypeConverter
    fun longListOfString(s: String) =
        s.split("?").filter { it != "" }.map { it.toLong() }

    @TypeConverter
    fun stringOfStringList(l: List<String>): String =
        l.fold("") { acc, s -> "$acc$s?" }

    @TypeConverter
    fun stringListOfString(s: String): List<String> =
        s.split("?").filter { it != "" }
}


@Dao
interface LibraryDAO {

    @Insert
    fun insert(libraryEntry: LibraryEntry)

    @Query("SELECT * FROM libraries")
    fun getAllLive(): LiveData<List<LibraryEntry>>

    @Query("SELECT * FROM libraries")
    fun getAll(): List<LibraryEntry>

    @Query("DELETE FROM libraries WHERE SOURCE = :source")
    fun deleteBySource(source: String)
}

@Dao
interface BookDAO {

    @Query("SELECT * FROM BOOKS")
    fun getAll(): List<BookEntry>

    @Query("SELECT * FROM BOOKS")
    fun getAllLive(): LiveData<List<BookEntry>>

    @Query("SELECT * FROM BOOKS WHERE SOURCE = :source")
    fun getBySource(source: String): BookEntry

    @Query("SELECT * FROM BOOKS WHERE SOURCE = :source")
    fun getBySourceLive(source: String): LiveData<BookEntry>

    @Query("DELETE FROM BOOKS WHERE SOURCE = :source ")
    fun deleteBySource(source: String)

    @Update
    fun update(book: List<BookEntry>)

    @Insert
    fun insert(book: BookEntry)

}


@Dao
interface BookmarkDAO {

    @Insert
    fun insert(bookmark: BookmarkEntry): Long

    @Update
    fun update(bookmark: BookmarkEntry)

    @Query("SELECT * FROM BOOKMARKS WHERE sourceBook = :source")
    fun getBySourceLive(source: String): LiveData<BookmarkEntry>

    @Query("SELECT * FROM BOOKMARKS WHERE id = :key")
    fun getByKey(key: Long): BookmarkEntry

    @Query("SELECT * FROM BOOKMARKS WHERE id = :key")
    fun getByKeyLive(key:Long): LiveData<BookmarkEntry>
}

@Dao
interface FileDAO {

    @Insert
    fun insert(fileEntries: List<FileEntry>)

    @Update
    fun update(l: List<FileEntry>)

    @Query("SELECT * FROM FILES WHERE source = :source")
    fun get(source: String): FileEntry

    @Query("SELECT * FROM FILES WHERE source = :source")
    fun getLive(source: String): LiveData<FileEntry>

    @Query("SELECT * FROM FILES WHERE sourceBook = :sourceBook")
    fun getBySourceBook(sourceBook: String): List<FileEntry>

    @Query("SELECT * FROM FILES WHERE sourceBook = :sourceBook")
    fun getBySourceBookLive(sourceBook: String): LiveData<MutableList<FileEntry>>

}


@Database(
    version = 1, exportSchema = false, entities =
    [BookEntry::class, LibraryEntry::class, BookmarkEntry::class, FileEntry::class]
)
@TypeConverters(Converters::class)
abstract class BookDatabase : RoomDatabase() {

    lateinit var context: Context
        private set

    abstract fun bookDao(): BookDAO
    abstract fun libraryDao(): LibraryDAO
    abstract fun fileDao(): FileDAO
    abstract fun bookmarkDao(): BookmarkDAO

    companion object {
        fun makeDB(context: Context): BookDatabase = Room.databaseBuilder(
            context.applicationContext, BookDatabase::class.java, Const.DB_NAME
        ).build().also{ it.context = context.applicationContext }
    }

}


