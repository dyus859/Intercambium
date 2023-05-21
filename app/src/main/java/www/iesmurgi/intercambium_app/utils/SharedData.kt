package www.iesmurgi.intercambium_app.utils

import androidx.lifecycle.LiveData
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.ui.viewmodels.SharedViewModel

// Need to use a shared view model to avoid destroying data when
// going to another activity
object SharedData {
    private val sharedViewModel = SharedViewModel()

    fun setUser(user: User) {
        sharedViewModel.setUser(user)
    }

    fun getUser(): LiveData<User> {
        return sharedViewModel.user
    }
}