package www.iesmurgi.intercambium_app.models

data class User(
    val email: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val photoUrl: String = ""
) : java.io.Serializable {

    constructor(other: User) : this(
        email = other.email,
        name = other.name,
        phoneNumber = other.phoneNumber,
        photoUrl = other.photoUrl
    )

}