package www.iesmurgi.intercambium_app.utils

import android.content.Context
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.findNavController
import www.iesmurgi.intercambium_app.BuildConfig
import www.iesmurgi.intercambium_app.models.Ad
import java.io.File
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
    fun getFileExtension(context: Context, uri: Uri): String? {
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
}