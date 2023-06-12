package www.iesmurgi.intercambium_app.models

data class Message(
    var messageId: String? = null,
    var content: String = "",
    var senderId: String = "",
    var imageUrl: String = "",
    var timeStamp: Long = 0,
    var deleted: Boolean = false,
    var type: Int = 0,
)