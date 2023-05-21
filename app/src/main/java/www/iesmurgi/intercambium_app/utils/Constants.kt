package www.iesmurgi.intercambium_app.utils

object Constants {
    // Document name for ads
    const val COLLECTION_ADS = "ads"

    // 'ads' document fields
    const val ADS_FIELD_AUTHOR = "author"
    const val ADS_FIELD_TITLE = "title"
    const val ADS_FIELD_DESCRIPTION = "description"
    const val ADS_FIELD_CREATED_AT = "createdAt"
    const val ADS_FIELD_IMAGE = "img"

    /************************************************************
     ************************************************************
     ************************************************************/

    // Document name for users
    const val COLLECTION_USERS = "users"

    // 'users' document fields
    const val USERS_FIELD_NAME = "name"
    const val USERS_FIELD_PHONE_NUMBER = "phoneNumber"
    const val USERS_FIELD_PHOTO_URL = "photoUrl"

    /************************************************************
     ************************************************************
     ************************************************************/

    // Minimum length of the title when publishing
    const val MIN_TITLE_LENGTH = 3

    // Minimum length of the description when publishing
    const val MIN_DESCRIPTION_LENGTH = 5

    const val STORAGE_FILE_FORMAT = "yyyy_MM_dd_HH_mm_ss"
    const val STORAGE_IMAGES_PATH = "images/"
}