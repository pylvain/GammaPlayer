package pylvain.gamma.library

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.*
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import pylvain.gamma.Const
import pylvain.gamma.MainApplication.Companion.context
import pylvain.gamma.logd
import java.io.File
import java.io.FileFilter
import java.lang.RuntimeException

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

class LibraryParser(val source: File) {
    val books: List<File> by lazy { scanFolder(source) }
    private fun scanFolder(dir: File): List<File> {
        val files: List<File> = dir.listFiles()?.toList() ?: emptyList()
        for (f in files) if (isAudioFile(f)) return listOf(dir)
        return files.filter { it.isDirectory }.flatMap { scanFolder(it) }
    }
}

class BookParser(val source: File) {

    val audioFiles: List<File> by lazy {
        source.listFiles (FileFilter { AudioFileParser.isAudio(it) })?.sortedBy { it.name }
            ?: emptyList()
    }

    fun delete() {}

    private fun extractCoverFromFile(): ByteArray? {
        val coverFiles = source.listFiles(FileFilter { isCoverFile(it) })?.sorted() ?: return null
        return if (coverFiles.isNotEmpty() && coverFiles[0].length() < Const.MAX_COVER_SIZE)
            coverFiles[0].readBytes() else null
    }

    private fun extractCoverFromMetadata(): ByteArray? {
        val file = audioFiles.sorted().getOrElse(0) { return null }
        return AudioFileParser(file).cover()
    }

    fun extractCover(): ByteArray? {
        return if (Const.PREFER_METADATA_COVER) extractCoverFromMetadata() ?: extractCoverFromFile()
        else extractCoverFromFile() ?: extractCoverFromMetadata()
    }

    fun getName(): String =
        audioFiles.firstOrNull()?.let { AudioFileParser(it).album() } ?: source.name

    fun computeTotalDuration() = audioFiles.map { AudioFileParser(it).duration().toLong() }.sum()
    fun computeSum() = audioFiles.map { it.length() }.sum()
}

class AudioFileParser(source: File) { //TODO redundant
    private val m = MediaMetadataRetriever()

    init { m.setDataSource(context, source.toUri()) }

    companion object {
        fun isAudio(source:File):Boolean { //Benchmark that
            val m = MediaMetadataRetriever()
            return isAudioFile(source) &&
                    runCatching { m.setDataSource(source.canonicalPath) }.isSuccess &&
                    m.extractMetadata(METADATA_KEY_HAS_AUDIO) != null
        }
    }



    fun duration(): Long = m.extractMetadata(METADATA_KEY_DURATION)?.toLong() ?: -1L
    fun cover(): ByteArray? = m.embeddedPicture
    fun nameFromMetadata(): String? = m.extractMetadata(METADATA_KEY_TITLE)
    fun album(): String? = m.extractMetadata(METADATA_KEY_ALBUM)
    fun title(): String? = m.extractMetadata(METADATA_KEY_TITLE)
}
