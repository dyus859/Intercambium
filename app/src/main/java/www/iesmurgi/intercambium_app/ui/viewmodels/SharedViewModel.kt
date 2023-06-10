package www.iesmurgi.intercambium_app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import www.iesmurgi.intercambium_app.models.User

/**
 * [ViewModel] class for sharing [User] data between components.
 * This [ViewModel] class is used to share [User] data between different components in the application.
 * It holds a [MutableLiveData] instance of [User] and provides methods to set and retrieve the [User] data.
 *
 * @constructor Creates an instance of [SharedViewModel].
 *
 * @author Denis Yushkin
 */
class SharedViewModel : ViewModel() {
    private val _user: MutableLiveData<User> = MutableLiveData()

    /**
     * LiveData object representing the User data.
     * This LiveData object is used to observe changes to the User data.
     * It provides read-only access to the User data.
     */
    val user: LiveData<User>
        get() = _user

    /**
     * Sets the User data in the ViewModel.
     * This method sets the User data in the ViewModel's MutableLiveData.
     * If the provided User is the same as the current User, the method returns without making any changes.
     *
     * @param user The User object to set.
     */
    fun setUser(user: User) {
        if (_user.equals(user)) {
            return
        }
        _user.value = user
    }
}