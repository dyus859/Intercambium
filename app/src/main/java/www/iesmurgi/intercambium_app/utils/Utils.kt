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
import java.text.SimpleDateFormat
import java.util.*


object Utils {
    fun getAdData(ad: Ad): HashMap<String, Any> {
        return hashMapOf(
            Constants.ADS_FIELD_TITLE to ad.title,
            Constants.ADS_FIELD_DESCRIPTION to ad.description,
            Constants.ADS_FIELD_PROVINCE to ad.province,
            Constants.ADS_FIELD_STATUS to ad.status,
            Constants.ADS_FIELD_CREATED_AT to ad.createdAt,
            Constants.ADS_FIELD_IMAGE to ad.imgUrl,
            Constants.ADS_FIELD_AUTHOR to ad.author.email
        )
    }

    fun isAdVisibleForUser(ad: Ad): Boolean {
        val user = SharedData.getUser().value!!
        val status = ad.status
        val author = ad.author

        return user.administrator
                || status == Constants.AD_STATUS_PUBLISHED
                || status == Constants.AD_STATUS_REVISION && author.equals(user)
    }

    fun getFileExtension(context: Context, uri: Uri): String? {
        val document = DocumentFile.fromSingleUri(context, uri)
        return document?.name?.let { name ->
            val extension = name.substringAfterLast(".", "")
            if (extension.isNotEmpty()) extension else null
        }
    }

    fun getImgPath(context: Context, uri: Uri): String {
        val formatter = SimpleDateFormat(Constants.STORAGE_FILE_FORMAT, Locale.getDefault())
        return (Constants.STORAGE_IMAGES_PATH
                + formatter.format(Date())
                + "."
                + getFileExtension(context, uri))
    }

    fun getTmpFileUri(context: Context): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
    }

    fun navigateToFragment(view: View?, resId: Int) {
        val navController = view?.findNavController()
        navController?.popBackStack()
        navController?.navigate(resId)
    }
}