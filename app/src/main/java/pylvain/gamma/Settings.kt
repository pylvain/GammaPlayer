package pylvain.gamma

import android.content.Context

interface SettingsProvider {
    fun get(key: String): Any
    fun set(key: String, value: Any)
}


class Settings(context: Context) {


    init {

    }

}