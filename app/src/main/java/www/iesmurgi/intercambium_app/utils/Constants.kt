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

    /**
     * Field name for the title search array of an ad document.
     */
    const val ADS_FIELD_TITLE_SEARCH = "title_search"

    /**
     * Field name for the description search array of an ad document.
     */
    const val ADS_FIELD_DESCRIPTION_SEARCH = "description_search"

    /**
     * Status of an ad document in the "in revision" state.
     */
    const val AD_STATUS_IN_REVISION = "in_revision"

    /**
     * Status of an ad document in the "published" state.
     */
    const val AD_STATUS_PUBLISHED = "published"

    /************************************************************
     ************************************************************
     ************************************************************/

    /**
     * Document name for users collection.
     */
    const val COLLECTION_USERS = "users"

    /**
     * Field name for the uid of a user document.
     */
    const val USERS_FIELD_UID = "uid"

    /**
     * Field name for the name of a user document.
     */
    const val USERS_FIELD_NAME = "name"

    /**
     * Field name for the photo URL of a user document.
     */
    const val USERS_FIELD_PHOTO_URL = "photoUrl"

    /**
     * Field name for the online status of a user document.
     */
    const val USERS_FIELD_ONLINE = "online"

    /**
     * Field name for the last active time of a user document.
     */
    const val USERS_FIELD_LAST_ACTIVE = "lastActive"

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
     * Path for storing ads in the storage.
     */
    const val STORAGE_ADS_IMAGES_PATH = "ads/"

    /**
     * Path for storing users profile photo in the storage.
     */
    const val STORAGE_USERS_IMAGES_PATH = "users/"

    /**
     * Path for storing message images in the storage.
     */
    const val STORAGE_CHATS_IMAGES_PATH = "chats/"

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
     * Document name for chats collection.
     */
    const val COLLECTION_CHATS = "chats"

    /**
     * Collection name for chat messages.
     */
    const val CHATS_COLLECTION_MESSAGES = "messages"

    /**
     * Field name for the message participants of a message document.
     */
    const val CHATS_FIELD_PARTICIPANTS = "participants"

    /**
     * Field name for the message content of a message document.
     */
    const val CHATS_FIELD_CONTENT = "content"

    /**
     * Field name for the message time of a message document.
     */
    const val CHATS_FIELD_TIME = "time"

    /**
     * Field name for the sender uid of a message document.
     */
    const val CHATS_FIELD_SENDER_UID = "senderUid"

    /**
     * Field name for the image url of a message document.
     */
    const val CHATS_FIELD_IMAGE_URL = "imageUrl"

    /**
     * Field name for the deleted status of a message document.
     */
    const val CHATS_FIELD_DELETED = "deleted"

    const val CHAT_SEND_ID = 1
    const val CHAT_RECEIVE_ID = 2
}