package www.iesmurgi.intercambium_app.models

/**
 * Data class representing a User.
 *
 * @property uid The ID of the user.
 * @property email The email address of the user.
 * @property name The name of the user.
 * @property photoUrl The URL of the user's photo.
 * @property online A flag indicating whether the user is currently online.
 * @property nameSearch A list of searchable names associated with the user.
 * @property fcmToken The FCM token of the user for push notifications.
 * @property administrator A flag indicating whether the user is an administrator.
 * @constructor Creates a new instance of the User data class.
 *
 * @author Denis Yushkin
 */
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