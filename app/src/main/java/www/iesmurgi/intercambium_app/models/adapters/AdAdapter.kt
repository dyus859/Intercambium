package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ItemAdBinding
import www.iesmurgi.intercambium_app.models.Ad
import www.iesmurgi.intercambium_app.utils.Constants

/**
 * [androidx.recyclerview.widget.RecyclerView] adapter for displaying ads.
 *
 * @param context The context in which the adapter is used.
 * @param onItemClick Callback function to handle item click events.
 *
 * @author Denis Yushkin
 */
class AdAdapter(
    private val context: Context,
    private val onItemClick: (Ad)->Unit
) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    /**
     * List of [Ad] objects to be displayed.
     */
    val adList = mutableListOf<Ad>()

    /**
     * [androidx.recyclerview.widget.RecyclerView.ViewHolder] class for ads.
     *
     * @param itemBinding The binding object for the item view.
     */
    inner class AdViewHolder(private val itemBinding: ItemAdBinding):
        RecyclerView.ViewHolder(itemBinding.root) {

        /**
         * Binds an ad to the view holder.
         *
         * @param ad The [Ad] object to bind.
         */
        fun bind(ad: Ad) = with(itemBinding) {
            if (ad.status == Constants.AD_STATUS_IN_REVISION) {
                cvAdContainer.setBackgroundColor(ContextCompat.getColor(itemView.context,
                    R.color.pale_yellow))
                ivAdHidden.visibility = View.VISIBLE
            } else {
                cvAdContainer.setBackgroundColor(ContextCompat.getColor(itemView.context,
                    R.color.light_gray))
                ivAdHidden.visibility = View.GONE
            }

            tvItemAdTitle.text = ad.title
            tvItemAdDescription.text = ad.description
            tvItemAdUserName.text = ad.author.name

            if (ad.imgUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(ad.imgUrl)
                    .placeholder(R.drawable.no_image)
                    .into(sivItemAdImage)
            } else {
                sivItemAdImage.setImageResource(R.drawable.no_image)
            }

            if (ad.author.photoUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(ad.author.photoUrl)
                    .placeholder(R.drawable.default_avatar)
                    .into(sivItemAdUserPhoto)
            } else {
                sivItemAdUserPhoto.setImageResource(R.drawable.default_avatar)
            }

            // When user clicks on the CardView
            root.setOnClickListener {
                onItemClick(adList[adapterPosition])
            }
        }
    }

    /**
     * Creates a new [AdViewHolder] by inflating the item layout.
     *
     * @param parent The [ViewGroup] into which the new [View] will be added.
     * @param viewType The view type of the new [View].
     * @return A new [AdViewHolder] that holds the inflated item view.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val itemBinding = ItemAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdViewHolder(itemBinding)
    }

    /**
     * Binds the data to the specified [AdViewHolder].
     *
     * @param holder The [AdViewHolder] to bind data to.
     * @param position The position of the item in the data set.
     */
    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        holder.bind(adList[position])
    }

    /**
     * Returns the total number of items in the data set.
     *
     * @return The total number of items in the [adList].
     */
    override fun getItemCount(): Int = adList.size

}