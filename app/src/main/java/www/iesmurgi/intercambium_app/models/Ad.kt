package www.iesmurgi.intercambium_app.models

import com.google.firebase.Timestamp

data class Ad(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var imgUrl: String = "",
    var author: User = User(),
    var loaded: Boolean = false
) : java.io.Serializable
