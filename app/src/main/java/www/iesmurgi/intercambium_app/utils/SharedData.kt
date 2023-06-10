package www.iesmurgi.intercambium_app.utils

import androidx.lifecycle.LiveData
import www.iesmurgi.intercambium_app.models.User
import www.iesmurgi.intercambium_app.ui.viewmodels.SharedViewModel

/**
 * Singleton object for storing and accessing shared data using a shared view model.
 * It's used to avoid destroying data when going to another activity.
 *
 * @author Denis Yushkin
 */
object SharedData {
    private val sharedViewModel = SharedViewModel()

    /**
     * Sets the user in the shared view model.
     *
     * @param user The [User] to be set.
     */
    fun setUser(user: User) {
        sharedViewModel.setUser(user)
    }

    /**
     * Retrieves the user from the shared view model as a [LiveData] object.
     *
     * @return The [LiveData] object containing the user.
     */
    fun getUser(): LiveData<User> {
        return sharedViewModel.user
    }
}