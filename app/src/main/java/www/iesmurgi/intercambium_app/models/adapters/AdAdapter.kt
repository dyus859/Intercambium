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
import www.iesmurgi.intercambium_app.utils.Utils

class AdAdapter(
    private val context: Context,
    private val onItemClick: (Ad)->Unit
) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    val adList = mutableListOf<Ad>()

    inner class AdViewHolder(private val itemBinding: ItemAdBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(ad: Ad) = with(itemBinding) {
            val canUserSeeAd = Utils.isAdVisibleForUser(ad)

            if (!canUserSeeAd) {
                itemView.visibility = View.GONE
                return
            }
            itemView.visibility = View.VISIBLE

            if (ad.status == Constants.AD_STATUS_REVISION || ad.status == Constants.AD_STATUS_HIDDEN) {
                cvAdContainer.setBackgroundColor(ContextCompat.getColor(itemView.context,
                    R.color.light_gray))
                ivAdHidden.visibility = View.VISIBLE
            } else {
                ivAdHidden.visibility = View.GONE
            }

            tvItemAdTitle.text = ad.title
            tvItemAdDescription.text = ad.description
            tvItemAdUserName.text = ad.author.name

            if (ad.imgUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(ad.imgUrl)
                    .into(sivItemAdImage)
            } else {
                sivItemAdImage.setImageResource(R.drawable.no_image)
            }

            if (ad.author.photoUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(ad.author.photoUrl)
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdViewHolder {
        val itemBinding = ItemAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AdViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: AdViewHolder, position: Int) {
        holder.bind(adList[position])
    }

    override fun getItemCount(): Int = adList.size

    fun getVisibleAdsCount(): Int = adList.count { it.visible }

}