package www.iesmurgi.intercambium_app.models.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import www.iesmurgi.intercambium_app.R
import www.iesmurgi.intercambium_app.models.Province

class ProvinceAdapter(
    context: Context,
    provinces : List<Province>
) : ArrayAdapter<Province>(context,0, provinces) {
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