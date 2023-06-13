package www.iesmurgi.intercambium_app.models

data class Chat(
    var id: String = "",
    val receiverUser: User = User(),
    val lastMsg: String = "",
    val lastImageUrl: String = "",
) : java.io.Serializable
