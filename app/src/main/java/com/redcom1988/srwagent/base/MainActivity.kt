package com.redcom1988.srwagent.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.util.Consumer
import androidx.lifecycle.lifecycleScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.ScreenTransition
import com.redcom1988.domain.auth.interactor.RefreshToken
import com.redcom1988.domain.auth.repository.TokenStorage
import com.redcom1988.srwagent.screens.home.HomeScreen
import com.redcom1988.srwagent.screens.login.LoginScreen
import com.redcom1988.srwagent.theme.AppTheme
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

class MainActivity : ComponentActivity() {

    private val tokenStorage: TokenStorage by inject()
    private val refreshToken: RefreshToken by inject()

    private var isReady = false
    private var initialScreen: Screen = LoginScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    return if (isReady) {
                        content.viewTreeObserver.removeOnPreDrawListener(this)
                        true
                    } else {
                        false
                    }
                }
            }
        )

        handlePreDraw()

        enableEdgeToEdge()
        setContent {
            AppTheme {
                val slideDistance = rememberSlideDistance()
                Navigator(
                    screen = initialScreen,
                    disposeBehavior = NavigatorDisposeBehavior(
                        disposeNestedNavigators = false,
                        disposeSteps = true,
                    )
                ) { navigator ->
                    ScreenTransition(
                        modifier = Modifier.fillMaxSize(),
                        navigator = navigator,
                        transition = {
                            materialSharedAxisX(
                                forward = navigator.lastEvent != StackEvent.Pop,
                                slideDistance = slideDistance,
                            )
                        },
                    )
                    HandleNewIntent(this@MainActivity, navigator)
                }
            }
        }
    }

    private fun handlePreDraw() {
        lifecycleScope.launch {
            val existingRefreshToken = tokenStorage.getRefreshToken()

            if (existingRefreshToken.isNullOrEmpty()) {
                initialScreen = LoginScreen
            } else {
                when (val result = refreshToken.await()) {
                    is RefreshToken.Result.Success -> {
                        initialScreen = HomeScreen
                    }
                    is RefreshToken.Result.Error -> {
                        tokenStorage.clearTokens()
                        initialScreen = LoginScreen
                    }
                }
            }

            isReady = true
        }
    }

    @Composable
    private fun HandleNewIntent(context: Context, navigator: Navigator) {
        LaunchedEffect(Unit) {
            callbackFlow {
                val componentActivity = context as ComponentActivity
                val consumer = Consumer<Intent> { trySend(it) }
                componentActivity.addOnNewIntentListener(consumer)
                awaitClose { componentActivity.removeOnNewIntentListener(consumer) }
            }.collectLatest { handleIntentAction(it, navigator) }
        }
    }

    private fun handleIntentAction(intent: Intent, navigator: Navigator) {
        // Handle intent here
    }
}
