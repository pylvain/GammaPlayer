package pylvain.gamma

import org.junit.Test
import java.io.File

class UT {
    @Test
    fun f1() {
        val f = File("/sdcard/Audiobooks")
        assert(f.contains(f))
    }
}