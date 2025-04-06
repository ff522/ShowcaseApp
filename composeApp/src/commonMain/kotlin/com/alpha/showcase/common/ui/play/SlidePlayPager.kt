package com.alpha.showcase.common.ui.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.alpha.showcase.common.ui.settings.SHOWCASE_MODE_SLIDE
import getPlatform
import isIos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import showcaseapp.composeapp.generated.resources.Res
import showcaseapp.composeapp.generated.resources.backward
import showcaseapp.composeapp.generated.resources.forward
import kotlin.math.absoluteValue

@Composable
fun SlideImagePager(
  imageList: List<Any>,
  vertical: Boolean = false,
  fitSize: Boolean = false,
  switchDuration: Long = DEFAULT_PERIOD,
  showProgress: Boolean = true,
  showContentInfo: Boolean = false
) {

  val pagerState = rememberPagerState {
    imageList.size
  }
  val focusRequester = remember { FocusRequester() }

  var currentData by remember {
    mutableStateOf<Any?>(null)
  }


  var showOpButton by remember { mutableStateOf(false) }

  LaunchedEffect(showOpButton) {
    if (showOpButton) {
      delay(5000) // Wait for 10 seconds
      showOpButton = false // Hide the button
    }
  }

  val loadComplete by remember {
    derivedStateOf {
      currentData != null
    }
  }

  Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
    // Listen for pointer (mouse) movements
    awaitPointerEventScope {
      while (true) {
        val event = awaitPointerEvent()
        if (event.changes.isNotEmpty()) {
          showOpButton = true
        }
      }
    }
  }) {
    if (vertical) {
      VerticalPager(
        state = pagerState, modifier = Modifier
          .fillMaxSize()
          .focusRequester(focusRequester)
      ) { page ->

        // Calculate the absolute offset for the current page from the
        // scroll position. We use the absolute value which allows us to mirror
        // any effects for both directions
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        PagerCard(pageOffset) {
          PagerItem(data = imageList[page], fitSize = fitSize, parentType = SHOWCASE_MODE_SLIDE) {
            currentData = it
            showOpButton = false
          }
        }
      }
    } else {
      HorizontalPager(
        state = pagerState, modifier = Modifier
          .fillMaxSize()
          .focusRequester(focusRequester)
      ) { page ->

        // Calculate the absolute offset for the current page from the
        // scroll position. We use the absolute value which allows us to mirror
        // any effects for both directions
        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
        PagerCard(pageOffset) {
          PagerItem(data = imageList[page], fitSize = fitSize, parentType = SHOWCASE_MODE_SLIDE) {
            currentData = it
          }
        }
      }
    }


    var progress by remember { mutableFloatStateOf(-1f) }
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState) {
      snapshotFlow { pagerState.currentPage }.collect { _ ->
        progress = 0f
        currentPage = pagerState.currentPage
      }
    }
    val progressAnimationValue by animateFloatAsState(
      targetValue = progress,
      animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
      label = "progress animateFloat"
    )

    AnimatedVisibility(showProgress
            && !pagerState.isScrollInProgress
            && imageList.size > 1
            && !imageList[currentPage % imageList.size].isVideo() && progress > 0,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.BottomCenter),
    ) {

      LinearProgressIndicator(
        progress = {
          progressAnimationValue / switchDuration.toFloat()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(2.dp)
          .align(Alignment.BottomCenter),
      )
    }

    LaunchedEffect(Unit){
      while (isActive) {
        delay(100)
        if (!pagerState.isScrollInProgress) {
          if (progress > switchDuration + 100 && !imageList[currentPage % imageList.size].isVideo()) {
            try {
              if (pagerState.canScrollForward) {
                pagerState.animateScrollToPage(
                  page = pagerState.currentPage + 1,
                  animationSpec = tween(1500)
                )
              } else {
                pagerState.animateScrollToPage(
                  page = 0
                )
              }
            }catch (e: CancellationException){
              e.printStackTrace()
            }

            delay(300)
          } else {
            if (!pagerState.isScrollInProgress) {
              progress += 100
            }
          }
        }

      }
    }
    ChangePage(pagerState, showOpButton)
  }
}

@Composable
fun ChangePage(
  pagerState: PagerState,
  show: Boolean
) {

  if (!isIos()){
    Box(modifier = Modifier.fillMaxSize()) {
      val animationScope = rememberCoroutineScope()
      AnimatedVisibility(
        show && pagerState.canScrollForward,
        modifier = Modifier.align(Alignment.CenterEnd),
        enter = fadeIn(),
        exit = fadeOut()
      ){
        IconButton(
          onClick = {
            animationScope.launch {
              if (pagerState.canScrollForward && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                  page = pagerState.currentPage + 1,
                  animationSpec = tween(1000)
                )
              } else {
                pagerState.animateScrollToPage(
                  page = 0
                )
              }
            }
          },
          modifier = Modifier.padding(30.dp).focusable().background(Color.Gray.copy(0.5f), shape = CircleShape)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = stringResource(Res.string.forward),
            tint = Color.Black.copy(0.5f)
          )
        }
      }

      AnimatedVisibility(
        show && pagerState.canScrollBackward,
        modifier = Modifier.align(Alignment.CenterStart),
        enter = fadeIn(),
        exit = fadeOut()
      ){
        IconButton(
          onClick = {
            animationScope.launch {
              if (pagerState.canScrollBackward) {
                pagerState.animateScrollToPage(
                  page = pagerState.currentPage - 1,
                  animationSpec = tween(1000)
                )
              }
            }
          },
          modifier = Modifier.padding(30.dp).focusable().background(Color.Gray.copy(0.5f), shape = CircleShape)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
            contentDescription = stringResource(Res.string.backward),
            tint = Color.Black.copy(0.5f)
          )
        }
      }
    }
  }

}

@Composable
fun PagerCard(pageOffset: Float, content: @Composable ColumnScope.() -> Unit) {
  Card(
    Modifier
      .graphicsLayer {
        // Calculate the absolute offset for the current page from the
        // scroll position. We use the absolute value which allows us to mirror
        // any effects for both directions
        // We animate the scaleX + scaleY, between 85% and 100%
        lerp(
            0.8f,
            1f,
            1f - pageOffset.coerceIn(0f, 1f)
          )
          .also { scale ->
            scaleX = scale
            scaleY = scale
          }

        // We animate the alpha, between 50% and 100%
        alpha = lerp(
          0.5f,
          1f,
          1f - pageOffset.coerceIn(0f, 1f)
        )
      }
      .fillMaxSize(),
    shape = RectangleShape) {
    content()
  }
}