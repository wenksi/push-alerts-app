package ch.wenksi.pushalerts.viewModels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import ch.wenksi.pushalerts.errors.AuthenticationError
import ch.wenksi.pushalerts.models.Credentials
import ch.wenksi.pushalerts.models.Token
import ch.wenksi.pushalerts.repositories.UserRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for user data to encapsulate this logic from the user interface (fragments).
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UserRepository()

    var token: LiveData<Token> = repository.token
    var error: LiveData<String> = repository.error

    /**
     * Logs in the user with the email and password using a UserRepository.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val credentials = Credentials(email, password)
                repository.login(credentials)
            } catch (error: AuthenticationError) {
                Log.e("LOGIN", error.message.toString())
            }
        }
    }
}
