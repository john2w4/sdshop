package com.sdshop.client.app

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun MainViewController(
    environmentFactory: () -> AppEnvironment = { AppEnvironmentFactory.create() }
): UIViewController = ComposeUIViewController {
    SdShopApp(environmentFactory)
}
