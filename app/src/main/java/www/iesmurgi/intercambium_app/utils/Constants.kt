package www.iesmurgi.intercambium_app.utils

/**
 * Constants used in the application for document names and field names.
 *
 * @author Denis Yushkin
 */
object Constants {
    /**
     * Document name for ads collection.
     */
    const val COLLECTION_ADS = "ads"

    /**
     * Field name for the author of an ad document.
     */
    const val ADS_FIELD_AUTHOR = "author"

    /**
     * Field name for the title of an ad document.
     */
    const val ADS_FIELD_TITLE = "title"

    /**
     * Field name for the description of an ad document.
     */
    const val ADS_FIELD_DESCRIPTION = "description"

    /**
     * Field name for the province of an ad document.
     */
    const val ADS_FIELD_PROVINCE = "province"

    /**
     * Field name for the status of an ad document.
     */
    const val ADS_FIELD_STATUS = "status"

    /**
     * Field name for the creation timestamp of an ad document.
     */
    const val ADS_FIELD_CREATED_AT = "createdAt"

    /**
     * Field name for the image URL of an ad document.
     */
    const val ADS_FIELD_IMAGE = "img"

    /************************************************************
     ************************************************************
     ************************************************************/

    /**
     * Document name for users collection.
     */
    const val COLLECTION_USERS = "users"

    /**
     * Field name for the name of a user document.
     */
    const val USERS_FIELD_NAME = "name"

    /**
     * Field name for the age of a user document.
     */
    const val USERS_FIELD_AGE = "age"

    /**
     * Field name for the phone number of a user document.
     */
    const val USERS_FIELD_PHONE_NUMBER = "phoneNumber"

    /**
     * Field name for the photo URL of a user document.
     */
    const val USERS_FIELD_PHOTO_URL = "photoUrl"

    /**
     * Field name for the administrator status of a user document.
     */
    const val USERS_FIELD_ADMINISTRATOR = "administrator"

    /************************************************************
     ************************************************************
     ************************************************************/

    /**
     * Minimum length of the title when publishing an ad.
     */
    const val MIN_TITLE_LENGTH = 3

    /**
     * Maximum length of the title when publishing an ad.
     */
    const val MAX_TITLE_LENGTH = 64

    /**
     * Minimum length of the description when publishing an ad.
     */
    const val MIN_DESCRIPTION_LENGTH = 5

    /**
     * Maximum length of the description when publishing an ad.
     */
    const val MAX_DESCRIPTION_LENGTH = 256

    /**
     * Format for storing images in the storage.
     */
    const val STORAGE_FILE_FORMAT = "yyyy_MM_dd_HH_mm_ss"

    /**
     * Path for storing images in the storage.
     */
    const val STORAGE_IMAGES_PATH = "images/"

    /************************************************************
     ************************************************************
     ************************************************************/

    /**
     * Minimum length of the password required for sign up.
     */
    const val MIN_PASSWORD_LENGTH = 6

    /************************************************************
     ************************************************************
     ************************************************************/

    /**
     * Status of an ad document in the "in revision" state.
     */
    const val AD_STATUS_IN_REVISION = "in_revision"

    /**
     * Status of an ad document in the "published" state.
     */
    const val AD_STATUS_PUBLISHED = "published"
}