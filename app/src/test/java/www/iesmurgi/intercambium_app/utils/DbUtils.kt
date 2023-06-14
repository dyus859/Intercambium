import org.junit.Assert.assertEquals
import org.junit.Test
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.models.Message
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.utils.Constants
import www.iesmurgi.intercambium_app.utils.DbUtils

class DbUtilsTest {
    @Test
    fun testGetAdData_ReturnsCorrectHashMap() {
        // Create a sample Ad object
        val ad = Ad(
            id = "123",
            title = "Sample Ad",
            description = "This is a sample ad",
            rating = 4.5,
            province = "Sample Province",
            status = "Active",
            createdAt = 1623705931,
            imgUrl = "https://example.com/sample.png",
            author = User("456", "test@example.com", "John Doe", "", true, listOf(), "", false)
        )

        // Call the function to get the ad data as a HashMap
        val adData = DbUtils.getAdData(ad)

        // Verify the expected values in the HashMap
        assertEquals("Sample Ad", adData[Constants.ADS_FIELD_TITLE])
        assertEquals("This is a sample ad", adData[Constants.ADS_FIELD_DESCRIPTION])
        assertEquals(4.5, adData[Constants.ADS_FIELD_RATING])
        assertEquals("Sample Province", adData[Constants.ADS_FIELD_PROVINCE])
        assertEquals("Active", adData[Constants.ADS_FIELD_STATUS])
        assertEquals(1623705931L, adData[Constants.ADS_FIELD_CREATED_AT])
        assertEquals("https://example.com/sample.png", adData[Constants.ADS_FIELD_IMAGE])
        assertEquals("test@example.com", adData[Constants.ADS_FIELD_AUTHOR])
    }

    @Test
    fun testGetMessageData_ReturnsCorrectHashMap() {
        // Create a sample Message object
        val message = Message(
            messageId = "123",
            content = "Hello, world!",
            senderId = "456",
            imageUrl = "https://example.com/image.png",
            timeStamp = 1623705931,
            deleted = false
        )

        // Call the function to get the message data as a HashMap
        val messageData = DbUtils.getMessageData(message)

        // Verify the expected values in the HashMap
        assertEquals("Hello, world!", messageData[Constants.CHATS_FIELD_CONTENT])
        assertEquals("456", messageData[Constants.CHATS_FIELD_SENDER_UID])
        assertEquals("https://example.com/image.png", messageData[Constants.CHATS_FIELD_IMAGE_URL])
        assertEquals(1623705931L, messageData[Constants.CHATS_FIELD_TIME])
        assertEquals(false, messageData[Constants.CHATS_FIELD_DELETED])
    }
}