//package com.citacomsys.tem.design.components
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.unit.dp
//
//@Composable
//actual fun ThreePaneNavigationSuite(
//    modifier: Modifier,
//    navigationContent: @Composable () -> Unit,
//    listPane: @Composable () -> Unit,
//    detailPane: @Composable () -> Unit,
//    extraPane: @Composable () -> Unit
//) {
//    val state = remember { ThreePaneSuiteState() }
//
//    CompositionLocalProvider(LocalThreePaneSuiteState provides state) {
//        Row(modifier = modifier.fillMaxSize()) {
//            // 1. Navigation Content (Controls the display)
//            Box(modifier = Modifier.fillMaxHeight()) {
//                navigationContent()
//            }
//
//            VerticalDivider(thickness = 1.dp)
//
//            // Fixed List Pane
//            Box(modifier = Modifier.width(350.dp).fillMaxHeight()) {
//                listPane()
//            }
//
//            VerticalDivider(thickness = 1.dp)
//
//            // Dynamic Content Pane (Details or Extra)
//            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
//                when (state.currentDisplay) {
//                    ThreePaneDisplay.Details -> detailPane()
//                    ThreePaneDisplay.Extra -> extraPane()
//                }
//            }
//        }
//    }
//}
