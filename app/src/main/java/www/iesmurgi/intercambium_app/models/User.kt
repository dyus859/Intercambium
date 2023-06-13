package www.iesmurgi.intercambium_app.models

data class User(
    val uid: String = "",
    val email: String = "",
    var name: String = "",
    var photoUrl: String = "",
    var online: Boolean = true,
    var nameSearch: List<String> = emptyList(),
    var fcmToken: String = "",
    val administrator: Boolean = false,
) : java.io.Serializable {

    /**
     * Secondary constructor that copies the properties from another [User] object.
     *
     * @param other The other [User] object to copy the properties from.
     */
    constructor(other: User) : this(
        uid = other.uid,
        email = other.email,
        name = other.name,
        photoUrl = other.photoUrl,
        online = other.online,
        nameSearch = other.nameSearch,
        fcmToken = other.fcmToken,
        administrator = other.administrator,
    )

}