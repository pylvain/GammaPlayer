package pylvain.gamma

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.navGraphViewModels
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.experimental.dsl.viewModel
import org.koin.androidx.viewmodel.compat.ScopeCompat.viewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryUtils
import pylvain.gamma.player.BookPlayer
import pylvain.gamma.viewmodels.PlayerVM
import timber.log.Timber
import timber.log.Timber.DebugTree
import java.io.File


class MainActivity : AppCompatActivity() {

    private val bp by inject<BookPlayer>()
    private val libraryUtils by inject<LibraryUtils>()
    private val db by inject<BookDatabase>()


    private val playerVM:PlayerVM by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.i(db.isOpen().toString())

        runBlocking {
        CoroutineScope(Dispatchers.IO).launch {
            libraryUtils.apply {
                deleteDB()
                addLibrary(File("/storage/self/primary/Audiobooks"))
            }
            val books = db.bookDao().getAll()
            Timber.d("Found ${books.size} books")
            bp.loadBook(books[0].source)
            bp.play()
            bp.setSpeed(2f)
            bp.tickEnabled = true

            bp.next()
            //playerVM.onNext()
        }}

        Timber.i("MAIN")

        setContentView(R.layout.activity_main)
    }

}
