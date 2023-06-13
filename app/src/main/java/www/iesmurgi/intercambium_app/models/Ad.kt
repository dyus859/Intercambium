package www.iesmurgi.intercambium_app.models

import www.iesmurgi.intercambium_app.utils.Constants

/**
 * Represents an advertisement.
 *
 * @property id The unique identifier of the ad.
 * @property title The title of the ad.
 * @property description The description of the ad.
 * @property rating The rating of the ad.
 * @property province The province associated with the ad.
 * @property status The status of the ad.
 * @property createdAt The creation timestamp of the ad.
 * @property imgUrl The URL of the image associated with the ad.
 * @property author The user who created the ad.
 * @property loaded Indicates if the ad has been loaded.
 * @constructor Creates an instance of the [Ad] class.
 *
 * @author Denis Yushkin
 */
data class Ad(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var rating: Double = 0.0,
    var province: String = "",
    var status: String = Constants.AD_STATUS_IN_REVISION,
    @Transient
    var createdAt: Long = java.util.Date().time,
    var imgUrl: String = "",
    var author: User = User(),
    var loaded: Boolean = false,
) : java.io.Serializable
