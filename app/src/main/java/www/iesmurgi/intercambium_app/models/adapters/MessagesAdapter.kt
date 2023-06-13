package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.databinding.ItemMessageBinding
import www.iesmurgi.intercambium_app.models.Message
import www.iesmurgi.intercambium_app.utils.Constants

/**
 * Adapter for displaying messages in a RecyclerView.
 *
 * @param context The context of the adapter.
 * @param messages The list of messages to be displayed.
 * @param senderRoom The sender's room.
 * @param receiverRoom The receiver's room.
 * @param onItemLongClick The callback function for long item click events.
 *
 * @author Denis Yushkin
 */
class MessagesAdapter(
    private val context: Context,
    private val messages: ArrayList<Message>,
    private val senderRoom: String,
    private val receiverRoom: String,
    private val onItemLongClick: (View, Message) -> Unit,
): RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val itemBinding: ItemMessageBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        /**
         * Binds the message data to the item view.
         *
         * @param message The message to bind.
         */
        fun bind(message: Message) {
            with(itemBinding) {
                if (message.deleted) {
                    message.content = context.getString(R.string.this_message_is_deleted)
                }

                // Apply text style and color based on the isDeleted flag for sender and receiver text views
                applyTextStyleAndColor(chatMessageSender, message.deleted)
                applyTextStyleAndColor(chatMessageSender, message.deleted)

                // Handle the visibility and content of the sender and receiver views based on message type
                if (message.type == Constants.CHAT_SEND_ID) {
                    chatMainLayoutReceiver.visibility = View.GONE

                    if (message.content.isNotEmpty()) {
                        // Handle sender text message
                        chatLinearSender.visibility = View.VISIBLE
                        chatImgSender.visibility = View.GONE
                        chatMessageSender.text = message.content

                        // Set long click listener for sender text message
                        chatLinearSender.setOnLongClickListener {
                            onItemLongClick(it, message)
                            true
                        }
                    } else if (message.imageUrl.isNotEmpty()) {
                        // Handle sender image message
                        chatLinearSender.visibility = View.GONE
                        chatImgSender.visibility = View.VISIBLE

                        // Load image using Glide
                        Glide.with(context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.no_image)
                            .into(chatImgSender)

                        // Set long click listener for sender image message
                        chatImgSender.setOnLongClickListener {
                            onItemLongClick(it, message)
                            true
                        }
                    } else {
                        chatImgSender.visibility = View.GONE
                    }
                } else {
                    chatMainLayoutSender.visibility = View.GONE

                    if (message.content.isNotEmpty()) {
                        // Handle receiver text message
                        chatLinearReceiver.visibility = View.VISIBLE
                        chatImgReceiver.visibility = View.GONE
                        chatMessageReceiver.text = message.content

                        // Set long click listener for receiver text message
                        chatLinearReceiver.setOnLongClickListener {
                            onItemLongClick(it, message)
                            true
                        }
                    } else if (message.imageUrl.isNotEmpty()) {
                        // Handle receiver image message
                        chatLinearReceiver.visibility = View.GONE
                        chatImgReceiver.visibility = View.VISIBLE

                        // Load image using Glide
                        Glide.with(context)
                            .load(message.imageUrl)
                            .placeholder(R.drawable.no_image)
                            .into(chatImgReceiver)

                        // Set long click listener for receiver image message
                        chatImgReceiver.setOnLongClickListener {
                            onItemLongClick(it, message)
                            true
                        }
                    } else {
                        chatImgReceiver.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Inflate the item view layout using LayoutInflater
        val itemBinding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // Bind the message data to the view holder
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    /**
     * Applies the appropriate text style and color to a TextView based on the isDeleted flag.
     *
     * @param textView The TextView to apply the text style and color to.
     * @param isDeleted A boolean flag indicating if the text is marked as deleted.
     */
    private fun applyTextStyleAndColor(textView: TextView, isDeleted: Boolean) {
        val textStyle = if (isDeleted) {
            Typeface.ITALIC
        } else {
            Typeface.NORMAL
        }

        val textColor = if (isDeleted) {
            ContextCompat.getColor(textView.context, R.color.aqua_haze)
        } else {
            Color.WHITE
        }

        textView.setTypeface(null, textStyle)
        textView.setTextColor(textColor)
    }

}