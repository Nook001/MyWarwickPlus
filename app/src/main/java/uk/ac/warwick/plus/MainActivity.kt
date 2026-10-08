package uk.ac.warwick.plus

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import uk.ac.warwick.plus.auth.LoginActivity
import uk.ac.warwick.plus.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = application as PlusApplication
            val appearance by app.appearance.state.collectAsStateWithLifecycle()
            PlusTheme(appearance, app.appearance::update) {
                val model: TimetableViewModel = viewModel(factory = viewModelFactory {
                    initializer { TimetableViewModel(app.repository, hasNetwork = app::hasNetwork) }
                })
                val state by model.state.collectAsStateWithLifecycle()
                val login = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                    if (it.resultCode == Activity.RESULT_OK) model.refreshAfterSignIn()
                }
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
