package com.armsone.denimdex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armsone.denimdex.core.design.DenimTheme
import com.armsone.denimdex.ui.RootScreen

/**
 * Single activity host. DenimTheme forces the light color scheme regardless of the
 * system setting, matching the iOS `.preferredColorScheme(.light)` lock (handoff 3.1, 11.4).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DenimTheme {
                RootScreen(
                    catalogFixture = if (BuildConfig.DEBUG) {
                        intent.getStringExtra(EXTRA_CATALOG_FIXTURE)
                    } else {
                        null
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_CATALOG_FIXTURE = "com.armsone.denimdex.extra.CATALOG_FIXTURE"
    }
}
