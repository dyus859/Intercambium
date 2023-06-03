package www.iesmurgi.intercambium_app.models

data class Province(
    val name: String = "",
    val region: String = "",
) {
    companion object {
        val provinceSource = listOf(
            Province("Álava", "País Vasco"),
            Province("Albacete", "Castilla-La Mancha"),
            Province("Alicante", "Comunidad Valenciana"),
            Province("Almería", "Andalucía"),
            Province("Asturias", "Principado de Asturias"),
            Province("Ávila", "Castilla y León"),
            Province("Badajoz", "Extremadura"),
            Province("Barcelona", "Cataluña"),
            Province("Burgos", "Castilla y León"),
            Province("Cáceres", "Extremadura"),
            Province("Cádiz", "Andalucía"),
            Province("Cantabria", "Cantabria"),
            Province("Castellón", "Comunidad Valenciana"),
            Province("Ciudad Real", "Castilla-La Mancha"),
            Province("Córdoba", "Andalucía"),
            Province("La Coruña", "Galicia"),
            Province("Cuenca", "Castilla-La Mancha"),
            Province("Gerona", "Cataluña"),
            Province("Granada", "Andalucía"),
            Province("Guadalajara", "Castilla-La Mancha"),
            Province("Guipúzcoa", "País Vasco"),
            Province("Huelva", "Andalucía"),
            Province("Huesca", "Aragón"),
            Province("Islas Baleares", "Islas Baleares"),
            Province("Jaén", "Andalucía"),
            Province("León", "Castilla y León"),
            Province("Lérida", "Cataluña"),
            Province("Lugo", "Galicia"),
            Province("Madrid", "Comunidad de Madrid"),
            Province("Málaga", "Andalucía"),
            Province("Murcia", "Región de Murcia"),
            Province("Navarra", "Comunidad Foral de Navarra"),
            Province("Orense", "Galicia"),
            Province("Palencia", "Castilla y León"),
            Province("Las Palmas", "Canarias"),
            Province("Pontevedra", "Galicia"),
            Province("La Rioja", "La Rioja"),
            Province("Salamanca", "Castilla y León"),
            Province("Segovia", "Castilla y León"),
            Province("Sevilla", "Andalucía"),
            Province("Soria", "Castilla y León"),
            Province("Tarragona", "Cataluña"),
            Province("Santa Cruz de Tenerife", "Canarias"),
            Province("Teruel", "Aragón"),
            Province("Toledo", "Castilla-La Mancha"),
            Province("Valencia", "Comunidad Valenciana"),
            Province("Valladolid", "Castilla y León"),
            Province("Vizcaya", "País Vasco"),
            Province("Zamora", "Castilla y León"),
            Province("Zaragoza", "Aragón")
        )
    }

    override fun toString(): String {
        return name
    }
}

