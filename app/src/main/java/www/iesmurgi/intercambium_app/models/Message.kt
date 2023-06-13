package www.iesmurgi.intercambium_app.models

/**
 * Represents a message in the chat system.
 *
 * @property messageId The ID of the message.
 * @property content The content of the message.
 * @property senderId The ID of the sender of the message.
 * @property imageUrl The URL of an image attached to the message.
 * @property timeStamp The timestamp of when the message was sent.
 * @property deleted Indicates if the message has been deleted.
 * @property type The type of the message.
 *
 * @author Denis Yushkin
 */
data class Message(
    var messageId: String? = null,
    var content: String = "",
    var senderId: String = "",
    var imageUrl: String = "",
    var timeStamp: Long = 0,
    var deleted: Boolean = false,
    var type: Int = 0,
)