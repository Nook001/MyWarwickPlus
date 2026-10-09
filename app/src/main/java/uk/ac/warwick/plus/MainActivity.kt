package uk.ac.warwick.plus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.auth.LoginActivity
import uk.ac.warwick.plus.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PlusApplication
        app.followCache(lifecycleScope)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { app.updates.checkIfDue() }
        }
        setContent {
            val appearance by app.appearance.state.collectAsStateWithLifecycle()
            PlusTheme(appearance, app.appearance::update) {
                val model: TimetableViewModel = viewModel(factory = viewModelFactory {
                    initializer { TimetableViewModel(app.repository, hasNetwork = app::hasNetwork) }
                })
                val state by model.state.collectAsStateWithLifecycle()
                val login = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_OK) model.refreshAfterSignIn()
                }
                CompositionLocalProvider(LocalUpdates provides app.updates, LocalReminders provides app.reminders) {
                    PlusScreen(state, PlusActions(
                        refresh = model::refresh,
                        signIn = { login.launch(Intent(this, LoginActivity::class.java)) },
                        signOut = model::signOut,
                        refreshResource = model::refreshResource,
                        loadOlderMessages = model::loadMoreMessages,
                        consumeNotice = model::consumeNotice,
                        refreshHome = model::refreshHome,
                        setHomeVisible = model::setHomeVisible
                    ))
                }
            }
        }
    }
}
