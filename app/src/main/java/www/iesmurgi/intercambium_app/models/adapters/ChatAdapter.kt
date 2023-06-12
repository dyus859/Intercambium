package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ItemChatBinding
import www.iesmurgi.intercambium_app.models.Chat

/**
 * [androidx.recyclerview.widget.RecyclerView] adapter for displaying chats.
 *
 * @param context The context in which the adapter is used.
 * @param onItemClick Callback function to handle item click events.
 *
 * @author Denis Yushkin
 */
class ChatAdapter(
    private val context: Context,
    private val onItemClick: (Chat)->Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    /**
     * List of [Chat] objects to be displayed.
     */
    val chatsList = mutableListOf<Chat>()

    /**
     * [androidx.recyclerview.widget.RecyclerView.ViewHolder] class for chats.
     *
     * @param itemBinding The binding object for the item view.
     */
    inner class ChatViewHolder(private val itemBinding: ItemChatBinding):
        RecyclerView.ViewHolder(itemBinding.root) {

        /**
         * Binds a [Chat] object to the view holder.
         *
         * @param chat The [Chat] object to bind.
         */
        fun bind(chat: Chat) {
            with(itemBinding) {
                tvChatUserName.text = chat.receiverUser.name
                tvChatUserLastMessage.text = chat.lastMsg

                // Load receiver's profile picture
                if (chat.receiverUser.photoUrl.isNotEmpty()) {
                    Glide.with(context)
                        .load(chat.receiverUser.photoUrl)
                        .placeholder(R.drawable.no_image)
                        .into(ivChatUserPicture)
                }

                // When user clicks on the CardView
                root.setOnClickListener {
                    onItemClick(chat)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val itemBinding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatsList[position])
    }

    override fun getItemCount(): Int = chatsList.size

}