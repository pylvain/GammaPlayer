package pylvain.gamma

import android.app.Application
import android.content.Context
import com.google.android.exoplayer2.SimpleExoPlayer
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module
import pylvain.gamma.library.BookDatabase
import pylvain.gamma.library.LibraryUtils
import pylvain.gamma.player.BookPlayer
import pylvain.gamma.viewmodels.PlayerVM
import timber.log.Timber


class MainApplication :  Application() {

    companion object {
        lateinit var mainContext: Context // TODO remove that ?
    }

    override fun onCreate() {
        super.onCreate()

        mainContext = this

        val mod = module {

            single<BookDatabase> { BookDatabase.makeDB(androidContext()) }
            single<SimpleExoPlayer> { SimpleExoPlayer.Builder(androidContext()).build() }
            single<LibraryUtils> { LibraryUtils(get()) }
            single<BookPlayer> { BookPlayer(androidContext(),get(), get(), get()) } //factory?

            viewModel { PlayerVM(get()) }
        }

        startKoin {
            androidLogger()
            androidContext(this@MainApplication)
            modules(mod)
        }

        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        /*

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        */
    }
}