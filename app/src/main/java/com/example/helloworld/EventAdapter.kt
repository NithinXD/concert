package com.example.helloworld

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(private var events: List<Event>) : 
    RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view), 
        View.OnCreateContextMenuListener {
        val imageView: ImageView = view.findViewById(R.id.event_image)
        val titleView: TextView = view.findViewById(R.id.event_title)
        val artistView: TextView = view.findViewById(R.id.event_artist)
        val dateView: TextView = view.findViewById(R.id.event_date)
        val timeView: TextView = view.findViewById(R.id.event_time)
        val venueView: TextView = view.findViewById(R.id.event_venue)
        val priceView: TextView = view.findViewById(R.id.event_price)
        val timeSlotButton: Button = view.findViewById(R.id.select_time_slot_button)

        init {
            view.setOnCreateContextMenuListener(this)
        }

        override fun onCreateContextMenu(
            menu: ContextMenu?,
            v: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.add(0, 0, 0, "Copy Event Details")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.event_item, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        
        holder.titleView.text = event.name
        holder.artistView.text = event.artist
        holder.dateView.text = event.date
        holder.timeView.text = event.time
        holder.venueView.text = event.venue
        holder.priceView.text = "$${String.format("%.2f", event.price)}"

        try {
            // Extract number from event name (assuming names like "Event 1", "Event 2", etc.)
            val eventNumber = position + 1 // Use position + 1 to match image names
            val imageResourceName = "event_$eventNumber"
            val imageResourceId = holder.itemView.context.resources.getIdentifier(imageResourceName, "drawable", holder.itemView.context.packageName)
            if (imageResourceId != 0) {
                holder.imageView.setImageResource(imageResourceId)
            } else {
                // If specific event image not found, use placeholder
                holder.imageView.setImageResource(R.drawable.placeholder_image)
            }
        } catch (e: Exception) {
            // Handle image loading error
            holder.imageView.setImageResource(R.drawable.placeholder_image)
        }

        // Handle time slot selection
        holder.timeSlotButton.setOnClickListener { view ->
            showTimeSlotPopupMenu(view, event)
        }

        // Handle context menu item selection
        holder.itemView.setOnLongClickListener { view ->
            val clipboard = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val eventDetails = """
                Event: ${event.name}
                Artist: ${event.artist}
                Date: ${event.date}
                Time: ${event.time}
                Venue: ${event.venue}
                Price: $${String.format("%.2f", event.price)}
                Category: ${event.category}
                Available Seats: ${event.availableSeats}
            """.trimIndent()
            
            val clip = ClipData.newPlainText("Event Details", eventDetails)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(view.context, "Event details copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showTimeSlotPopupMenu(view: View, event: Event) {
        val popup = PopupMenu(view.context, view)
        val basePrice = event.price

        popup.menu.add(0, 0, 0, "Early Entry (${formatPrice(basePrice * 1.2)})")
        popup.menu.add(0, 1, 1, "Regular Entry (${formatPrice(basePrice)})")
        popup.menu.add(0, 2, 2, "VIP Access (${formatPrice(basePrice * 1.5)})")

        popup.setOnMenuItemClickListener { item ->
            val (timeSlot, price) = when (item.itemId) {
                0 -> Pair("Early Entry", basePrice * 1.2)
                1 -> Pair("Regular Entry", basePrice)
                2 -> Pair("VIP Access", basePrice * 1.5)
                else -> Pair("Regular Entry", basePrice)
            }

            // Launch EventDetailActivity with selected time slot and position
            val intent = Intent(view.context, EventDetailActivity::class.java).apply {
                putExtra("event", event)
                putExtra("timeSlot", timeSlot)
                putExtra("price", price)
                putExtra("position", events.indexOf(event)) // Add position
            }
            view.context.startActivity(intent)
            true
        }

        popup.show()
    }

    private fun formatPrice(price: Double): String {
        return "$${String.format("%.2f", price)}"
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }
}