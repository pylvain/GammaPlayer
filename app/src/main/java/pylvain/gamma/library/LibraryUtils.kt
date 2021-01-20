package pylvain.gamma.library

import android.net.Uri
import androidx.core.net.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import pylvain.gamma.*
import pylvain.gamma.MainApplication.Companion.mainContext
import timber.log.Timber
import java.io.File
import kotlin.system.measureTimeMillis

//TODO: remove File() from VM side
//TODO: Koin lazy ?

class LibraryUtils(val db: BookDatabase) {

    private val cover404 by lazy { Uri.parse("R.drawable.img404").toFile().canonicalPath }
    private val coverFolder =
        File(mainContext.cacheDir, Const.COVER_CACHE_FOLDER).apply { exists() || mkdir() }

    fun deleteDB() {
        db.context.deleteDatabase(Const.DB_NAME)
        db.close()
    }

    private fun addNewBook(sourceLibrary: String, sourceBookFolder: String) { // TODO TODO TODO
        try {
            val parser = BookParser(File(sourceBookFolder))
            val newEntry = (BookEntry(
                librarySource = sourceLibrary,
                source = sourceBookFolder,
                playlist = parser.audioFiles.map { it.canonicalPath }, // already sorted
                author = parser.getAuthor(),
                title = parser.getTitle(),
                dateAdded = epoch(),
                fileSizeSum = -1,
                totalDuration = -1,
                settings = BookSettings()
            ))

            db.bookDao().insert(newEntry) // first insert here because of foreign keys
            db.fileDao().insert(newEntry.playlist.map {
                FileEntry(
                    source = it,
                    kind = "file",
                    sourceBook = newEntry.source
                )
            })
            newEntry.settings.cursor = db.bookmarkDao().insert(
                BookmarkEntry(
                    file = newEntry.playlist[0],
                    sourceBook = sourceBookFolder,
                    time = 0L,
                    name = "Cursor",
                    tags = "IGNORE"
                )
            )
            db.bookDao().update(listOf(newEntry))
        } catch (e: Exception) {
            db.bookDao().deleteBySource(sourceBookFolder) //TODO
            throw e
        }
    }

    private fun quickSyncLibrary(libFolder: File) {

        val parsed: List<File> = scanFolderRecursive(libFolder)
        val fromDB: List<File> = db.bookDao().getAll().map { File(it.source) }

        val newBooks = parsed.minus(fromDB)
        if (newBooks.isNotEmpty()) {
            newBooks.forEach { addNewBook(libFolder.canonicalPath, it.canonicalPath) }
        }
    }

    fun addLibrary(libFolder: File) {
        db.libraryDao().getAll().map { File(it.source) }.forEach {
            if (it.contains(libFolder) || libFolder.contains(it)) return // throw Exception("NestedLibraries")
        }
        db.libraryDao().insert(LibraryEntry(source = libFolder.canonicalPath))
        quickSyncLibrary(libFolder)
    }

    private fun coverPathOf(bookFolder: File) =
        File(coverFolder, bookFolder.canonicalPath.hashCode().toString()).canonicalPath

    fun getCover(bookFolder: File) = File(coverPathOf(bookFolder)).apply {
        if (!exists()) BookParser(bookFolder.canonicalFile).extractCover()?.let { writeBytes(it) }
            ?: return cover404 //TODO reformat
    }.canonicalPath

    /*
        Updates DB one at a time in order to have a fluid UI with LiveData
        benchmark pmap
     */

    fun computeBookDuration(source: String) = measureTimeMillis {
        runBlocking {
            withContext(CoroutineScope(Dispatchers.IO).coroutineContext) {
                    db.fileDao().getBySourceBook(source).pmap {
                        it.duration = AudioFileParser(File(it.source)).duration()
                        it
                    }.also { db.fileDao().update(it) }

                    db.bookDao().getBySource(source).let {
                        it.totalDuration =
                            db.fileDao().getBySourceBook(source).map { f -> f.duration }.sum()
                        if (it.totalDuration < 1L) throw Exception("Null duration")
                        db.bookDao().update(listOf(it))
                        Timber.i("Duration: ${formatMs(it.totalDuration)}")
                    }
            }
        }
    }.let { Timber.i("Duration of $source computed in ${formatMs(it)}") }
}
