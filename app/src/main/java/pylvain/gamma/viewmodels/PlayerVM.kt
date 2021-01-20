package pylvain.gamma.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.transform
import pylvain.gamma.player.BookPlayer
import timber.log.Timber


/*
-> Pour l'instant sync est manuelle
-> Est ce que l'on suppose que le lecteur doit avoir une source ?
-> Sync every *x
-> Playlist position avec un listener ? -> OUI
 */


class PlayerVM(val bp: BookPlayer) : ViewModel() {

    val fileProgress = bp.messageFlow.transform { msg ->
        if (msg.localProgress.second != 0L ) {
            emit((1000 * msg.localProgress.first.toFloat() / msg.localProgress.second.toFloat()).toInt())
        }
    }

    fun test() { Timber.i("YEAH") }

    fun onPlay() = bp.togglePlayPause()
    fun onPrevious() = bp.previous(guard = true)
    fun onNext()  { bp.next() }
    fun onMinus2() {}
    fun onMinus1() {}
    fun onPlus1() {}
    fun onPlus2() {}

}

