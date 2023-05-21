package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ItemAdBinding
import www.iesmurgi.intercambium_app.models.Ad

class AdAdapter(
    private val context: Context,
    private val onItemClick: (Ad)->Unit
) : RecyclerView.Adapter<AdAdapter.AdViewHolder>() {

    var adList = mutableListOf<Ad>()

    inner class AdViewHolder(private val itemBinding: ItemAdBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(ad: Ad) = with(itemBinding) {
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

}