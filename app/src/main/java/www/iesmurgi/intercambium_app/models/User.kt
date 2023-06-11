package www.iesmurgi.intercambium_app.models

/**
 * Represents a user.
 *
 * @property email The email address of the user.
 * @property name The name of the user.
 * @property age The age of the user.
 * @property phoneNumber The phone number of the user.
 * @property photoUrl The URL of the user's photo.
 * @property administrator Indicates if the user is an administrator.
 * @constructor Creates an instance of the [User] class.
 *
 * @author Denis Yushkin
 */
data class User(
    val email: String = "",
    var name: String = "",
    var age: Long? = null,
    val phoneNumber: String = "",
    var photoUrl: String = "",
    val administrator: Boolean = false,
) : java.io.Serializable {

    /**
     * Secondary constructor that copies the properties from another [User] object.
     *
     * @param other The other [User] object to copy the properties from.
     */
    constructor(other: User) : this(
        email = other.email,
        name = other.name,
        phoneNumber = other.phoneNumber,
        photoUrl = other.photoUrl,
        administrator = other.administrator,
    )

}