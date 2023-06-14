package www.iesmurgi.intercambium_app.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import www.iesmurgi.intercambium_app.utils.Utils

class UtilsTest {
    @Test
    fun testFormatUnixTime() {
        val unixTime = 1623750000000L // June 15, 2021 12:00:00 AM UTC

        val formattedTime = Utils.formatUnixTime(unixTime)

        assertEquals("15/06/2021 11:40", formattedTime)
    }
}
