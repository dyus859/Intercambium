package www.iesmurgi.intercambium_app.models

/**
 * Data class representing a [Chat].
 *
 * @property id The ID of the chat.
 * @property receiverUser The receiver user of the chat.
 * @property lastMsg The last message in the chat.
 * @property lastImageUrl The URL of the last image sent in the chat.
 * @constructor Creates a new instance of the [Chat] data class.
 *
 * @author Denis Yushkin
 */
data class Chat(
    var id: String = "",
    val receiverUser: User = User(),
    val lastMsg: String = "",
    val lastImageUrl: String = "",
) : java.io.Serializable
