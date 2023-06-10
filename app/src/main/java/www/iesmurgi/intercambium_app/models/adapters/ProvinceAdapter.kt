package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.models.Province

/**
 * ArrayAdapter for displaying a list of provinces.
 *
 * @param context The context of the application.
 * @param provinces The list of provinces to be displayed.
 *
 * @author Denis Yushkin
 */
class ProvinceAdapter(
    context: Context,
    provinces : List<Province>
) : ArrayAdapter<Province>(context,0, provinces) {
    /**
     * Returns the view for the specified position in the data set.
     *
     * @param position The position of the item within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent The parent that this view will eventually be attached to.
     * @return The view corresponding to the data at the specified position.
     */
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView?: LayoutInflater
            .from(context)
            .inflate(R.layout.body_provinces, parent, false)

        getItem(position)?.let { province ->
            view.findViewById<TextView>(R.id.tv_province_body_name).text =
                String.format("%s (%s)", province.name, province.region)
        }

        return view
    }
}