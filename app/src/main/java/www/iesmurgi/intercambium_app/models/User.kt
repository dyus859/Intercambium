package www.iesmurgi.intercambium_app.models

data class User(
    val email: String = "",
    var name: String = "",
    var age: Long? = null,
    val phoneNumber: String = "",
    val photoUrl: String = "",
    val administrator: Boolean = false,
) : java.io.Serializable {

    constructor(other: User) : this(
        email = other.email,
        name = other.name,
        phoneNumber = other.phoneNumber,
        photoUrl = other.photoUrl,
        administrator = other.administrator,
    )

}