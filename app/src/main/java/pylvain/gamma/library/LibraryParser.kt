package pylvain.gamma.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.provider.Settings.Global.getString
import android.support.v4.media.MediaMetadataCompat
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import pylvain.gamma.Const
import pylvain.gamma.MainApplication.Companion.mainContext
import pylvain.gamma.R
import java.io.File
import java.io.FileFilter

/*
    Classes to get information about the content of the library of the user
    These are not linked against the SQL database
 */


fun mimeType(file: File): String? =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)

fun isAudioFile(file: File): Boolean =
    mimeType(file)?.matches(Regex("^audio/.*")) == true

fun isCoverFile(file: File): Boolean =
    mimeType(file)?.matches(Regex("^image/.*")) == true

fun scanFolderRecursive(rootDir: File): List<File> {
    val files: List<File> = rootDir.listFiles()?.toList() ?: emptyList()
    for (f in files) if (isAudioFile(f)) return listOf(rootDir)
    return files.filter { it.isDirectory }.flatMap { scanFolderRecursive(it) }
}

class BookParser(val source: File) {

    val audioFiles: List<File> by lazy {
        source.listFiles(FileFilter { AudioFileParser.isAudio(it) })?.sortedBy { it.name }
            ?: emptyList()
    }

    val firstParserOrNull:AudioFileParser? by lazy {
        audioFiles.firstOrNull()?.let {AudioFileParser(it)}
    }

    fun delete() {}

    private fun extractCoverFromFile(): ByteArray? {
        val coverFiles =
            source.listFiles(FileFilter { isCoverFile(it) })?.sorted() ?: return null
        return if (coverFiles.isNotEmpty() && coverFiles[0].length() < Const.MAX_COVER_SIZE)
            coverFiles[0].readBytes() else null
    }

    private fun extractCoverFromMetadata(): ByteArray? {
        val file = audioFiles.sorted().getOrElse(0) { return null }
        return AudioFileParser(file).cover()
    }

    fun extractCover(): ByteArray? {
        return if (Const.PREFER_METADATA_COVER) extractCoverFromMetadata()
            ?: extractCoverFromFile()
        else extractCoverFromFile() ?: extractCoverFromMetadata()
    }

    fun getTitle(): String = firstParserOrNull?.album() ?: source.name

    fun getAuthor(): String = firstParserOrNull?.author() ?: "Anon" //TODO -> String

    fun computeTotalDuration() =
        audioFiles.map { AudioFileParser(it).duration().toLong() }.sum()

    fun computeSum() = audioFiles.map { it.length() }.sum()
}

class AudioFileParser(source: File) {

    private val m = MediaMetadataRetriever()

    init {
        m.setDataSource(mainContext, source.toUri())
    }

    companion object {
        fun isAudio(source: File): Boolean { //Benchmark that
            val m = MediaMetadataRetriever()
            return isAudioFile(source) &&
                    runCatching { m.setDataSource(source.canonicalPath) }.isSuccess &&
                    m.extractMetadata(METADATA_KEY_HAS_AUDIO) != null
        }
    }

    fun duration(): Long = m.extractMetadata(METADATA_KEY_DURATION)?.toLong() ?: -1L
    fun cover(): ByteArray? = m.embeddedPicture
    fun nameFromMetadata(): String? = m.extractMetadata(METADATA_KEY_TITLE)
    fun author(): String? =  m.extractMetadata(METADATA_KEY_AUTHOR)
    fun album(): String? = m.extractMetadata(METADATA_KEY_ALBUM)
    fun title(): String? = m.extractMetadata(METADATA_KEY_TITLE)

}
