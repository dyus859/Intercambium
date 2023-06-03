package www.iesmurgi.intercambium_app.models

import com.google.firebase.Timestamp

data class Ad(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var province: String = "",
    var status: String = "",
    var createdAt: Timestamp = Timestamp.now(),
    var imgUrl: String = "",
    var author: User = User(),
    var loaded: Boolean = false,
    var visible: Boolean = true,
) : java.io.Serializable
