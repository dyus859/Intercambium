package www.iesmurgi.intercambium_app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import www.iesmurgi.intercambium_app.utils.Utils.isNetworkAvailable

class UtilsTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockConnectivityManager: ConnectivityManager

    @Mock
    private lateinit var mockNetwork: Network

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        `when`(mockContext.getSystemService(Context.CONNECTIVITY_SERVICE))
            .thenReturn(mockConnectivityManager)
        `when`(mockConnectivityManager.activeNetwork)
            .thenReturn(mockNetwork)
    }

    @Test
    fun testFormatUnixTime() {
        // 15/06/2021 11:40 UTC
        val unixTime = 1623750000000L

        val formattedTime = Utils.formatUnixTime(unixTime)

        assertEquals("15/06/2021 11:40", formattedTime)
    }

    @Test
    fun testIsNetworkAvailable_withNoActiveNetwork_shouldReturnFalse() {
        // Mock no active network
        `when`(mockConnectivityManager.activeNetwork).thenReturn(null)

        val result = isNetworkAvailable(mockContext)

        assertFalse(result)
    }

    @Test
    fun testIsNetworkAvailable_withCellularTransport_shouldReturnTrue() {
        // Mock active network with cellular transport
        val mockNetworkCapabilities = mock(NetworkCapabilities::class.java)
        `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(true)

        val result = isNetworkAvailable(mockContext)

        assertTrue(result)
    }

    @Test
    fun testIsNetworkAvailable_withWifiTransport_shouldReturnTrue() {
        // Mock active network with WiFi transport
        val mockNetworkCapabilities = mock(NetworkCapabilities::class.java)
        `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(true)

        val result = isNetworkAvailable(mockContext)

        assertTrue(result)
    }

    @Test
    fun testIsNetworkAvailable_withEthernetTransport_shouldReturnTrue() {
        // Mock active network with Ethernet transport
        val mockNetworkCapabilities = mock(NetworkCapabilities::class.java)
        `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            .thenReturn(true)

        val result = isNetworkAvailable(mockContext)

        assertTrue(result)
    }

    @Test
    fun testIsNetworkAvailable_withOtherTransport_shouldReturnFalse() {
        // Mock active network with unsupported transport
        val mockNetworkCapabilities = mock(NetworkCapabilities::class.java)
        `when`(mockConnectivityManager.getNetworkCapabilities(mockNetwork))
            .thenReturn(mockNetworkCapabilities)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            .thenReturn(false)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            .thenReturn(false)
        `when`(mockNetworkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            .thenReturn(false)

        val result = isNetworkAvailable(mockContext)

        assertFalse(result)
    }
}
