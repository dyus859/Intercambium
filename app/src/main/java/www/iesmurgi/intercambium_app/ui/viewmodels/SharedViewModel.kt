package www.iesmurgi.intercambium_app.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import www.iesmurgi.intercambium_app.models.User

class SharedViewModel : ViewModel() {
    private val _user: MutableLiveData<User> = MutableLiveData()

    val user: LiveData<User>
        get() = _user

    fun setUser(user: User) {
        if (_user.equals(user)) {
            return
        }
        _user.value = user
    }
}