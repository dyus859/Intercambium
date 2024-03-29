package www.iesmurgi.intercambium_app.utils

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.findNavController
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import www.iesmurgi.intercambium_app.BuildConfig
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.DialogConfirmationBinding
import www.iesmurgi.intercambium_app.models.Ad
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions used in the application.
 *
 * @author Denis Yushkin
 */
object Utils {
    /**
     * Checks if an ad should be visible to the user based on the ad's status and the user's role.
     *
     * @param ad The [Ad] to check visibility for.
     * @return `true` if the ad should be visible, `false` otherwise.
     */
    fun isAdVisibleForUser(ad: Ad): Boolean {
        val user = SharedData.getUser().value!!
        val status = ad.status
        val author = ad.author

        return user.administrator
                || status == Constants.AD_STATUS_PUBLISHED
                || status == Constants.AD_STATUS_IN_REVISION && author.equals(user)
    }

    /**
     * Retrieves the file extension of a given [Uri].
     *
     * @param context The context to use for accessing content providers.
     * @param uri The [Uri] of the file.
     * @return The file extension or `null` if not found.
     */
    private fun getFileExtension(context: Context, uri: Uri): String? {
        val document = DocumentFile.fromSingleUri(context, uri)
        return document?.name?.let { name ->
            val extension = name.substringAfterLast(".", "")
            if (extension.isNotEmpty()) extension else null
        }
    }

    /**
     * Generates the image path for storing an image in the storage.
     *
     * @param context The [Context] to use for accessing resources.
     * @param uri The [Uri] of the image.
     * @return The generated image path.
     */
    fun getImgPath(context: Context, uri: Uri, directory: String): String {
        // Generate a unique image ID
        val uniqueImageId = UUID.randomUUID().toString()

        return (directory
                + uniqueImageId
                + "."
                + getFileExtension(context, uri))
    }

    /**
     * Creates a temporary file and returns its [Uri].
     *
     * @param context The context to use for accessing resources.
     * @return The [Uri] of the temporary file.
     */
    fun getTmpFileUri(context: Context): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    /**
     * Navigates to a specified fragment using the given [View].
     *
     * @param view The [View] from which to navigate.
     * @param resId The ID of the destination fragment.
     */
    fun navigateToFragment(view: View?, resId: Int) {
        val navController = view?.findNavController()
        navController?.popBackStack()
        navController?.navigate(resId)
    }

    /**
     * Deletes an image file from Firebase Storage using the provided URL.
     *
     * @param url The URL of the image file to be deleted.
     */
    fun deleteFirebaseImage(url: String) {
        if (url.isEmpty()) {
            return
        }

        // Create a FirebaseStorage instance
        val storage = FirebaseStorage.getInstance()

        // Get the reference to the image file using the provided URL
        val imageRef = storage.getReferenceFromUrl(url)

        // Delete the image file
        imageRef.delete()
    }

    /**
     * Deletes user chats from Firebase Firestore.
     *
     * This function deletes the chat documents associated with the given UID, along with their messages and image files.
     *
     * @param uid The UID of the user whose chats are to be deleted.
     */
    fun deleteUserChats(uid: String) {
        val db = Firebase.firestore
        val chatsCollection = db.collection(Constants.COLLECTION_CHATS)
        chatsCollection.get().addOnSuccessListener { querySnapshot ->
            for (document in querySnapshot.documents) {
                if (document.id.contains(uid)) {
                    val chatDocument = document.reference
                    val messagesCollection = chatDocument.collection(Constants.CHATS_COLLECTION_MESSAGES)

                    messagesCollection.get().addOnSuccessListener {
                        for (messageDocument in it.documents) {
                            val imageUrl = messageDocument.getString(Constants.CHATS_FIELD_IMAGE_URL)

                            // Delete the chat img
                            if (imageUrl != null && imageUrl.isNotEmpty()) {
                                deleteFirebaseImage(imageUrl)
                            }

                            // Delete the message document
                            messageDocument.reference.delete()

                            // Delete the chat itself
                            document.reference.delete()
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a confirmation [AlertDialog] with the specified title and success callback.
     *
     * @param title The title of the alert dialog.
     */
    fun createAlertDialog(activity: Activity, title: String, view: View): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(activity)
        alertDialogBuilder.setTitle(title)
        alertDialogBuilder.setView(view)
        alertDialogBuilder.setIcon(R.mipmap.ic_launcher)
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()

        return alertDialog
    }

    /**
     * Creates a confirmation alert dialog with the specified title and onSuccess callback.
     *
     * @param title The title of the confirmation dialog.
     * @param onSuccess The callback function to be executed when the confirm button is clicked.
     */
    fun createConfirmationAlertDialog(activity: Activity, title: String, onSuccess: () -> Unit) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_confirmation, null)
        val dialogConfirmationBinding = DialogConfirmationBinding.bind(view)

        val alertDialog = createAlertDialog(activity, title, view)

        with(dialogConfirmationBinding) {
            btnCancelAction.setOnClickListener { alertDialog.dismiss() }
            btnConfirmAction.setOnClickListener {
                onSuccess()
                alertDialog.dismiss()
            }
        }
    }

    /**
     * Formats a Unix time value to a string representation of date and time.
     *
     * @param unixTime The Unix time value to be formatted.
     * @return The formatted string representing the date and time.
     */
    fun formatUnixTime(unixTime: Long): String {
        val date = Date(unixTime) // Convert Unix time to milliseconds
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return dateFormat.format(date)
    }

    /**
     * Checks if the network is available.
     *
     * @param context The context used to access system services.
     * @return `true` if the network is available, `false` otherwise.
     */
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Checks if the network is available and shows a toast message if it's not.
     *
     * @param context The context used to access system services.
     * @return `true` if the network is available, `false` otherwise.
     */
    fun checkAndShowNetworkNotAvailable(context: Context): Boolean {
        val isNetworkAvailable = isNetworkAvailable(context)

        if (!isNetworkAvailable) {
            val msg = context.getString(R.string.no_access_to_internet)
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }

        return isNetworkAvailable
    }
}