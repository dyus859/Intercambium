package www.iesmurgi.intercambium_app.utils

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.google.android.gms.common.SignInButton
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
            Constants.ADS_FIELD_CREATED_AT to ad.createdAt,
            Constants.ADS_FIELD_IMAGE to ad.imgUrl,
            Constants.ADS_FIELD_AUTHOR to ad.author.email
        )
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

    // https://stackoverflow.com/a/18644355
    fun setGooglePlusButtonText(signInButton: SignInButton, buttonText: String?) {
        // Find the TextView that is inside of the SignInButton and set its text
        for (i in 0 until signInButton.childCount) {
            val v: View = signInButton.getChildAt(i)
            if (v is TextView) {
                v.text = buttonText
                return
            }
        }
    }
}