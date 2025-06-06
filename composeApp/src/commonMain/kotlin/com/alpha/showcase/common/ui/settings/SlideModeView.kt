package com.alpha.showcase.common.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import com.alpha.showcase.common.ui.view.CheckItem
import com.alpha.showcase.common.ui.view.SlideItem
import com.alpha.showcase.common.ui.view.SwitchItem
import com.alpha.showcase.ui.WebStoriesRotateLeft
import org.jetbrains.compose.resources.stringResource
import showcaseapp.composeapp.generated.resources.Res
import showcaseapp.composeapp.generated.resources.auto_play
import showcaseapp.composeapp.generated.resources.display_mode
import showcaseapp.composeapp.generated.resources.interval_time_unit
import showcaseapp.composeapp.generated.resources.orientation
import showcaseapp.composeapp.generated.resources.show_time_progress_indicator
import showcaseapp.composeapp.generated.resources.slide_effect
import showcaseapp.composeapp.generated.resources.sort_rule

/**
 *   - Slide
 *
 *      - Display (Full screen, Fit screen)
 *      - Orientation (Horizontal, Vertical)
 *      - Auto play (Interval)
 *      - Interval time
 *      - Interval time unit
 *      - Show time progress
 *      - Show content meta info
 *      - Sort rule
 */
@Composable
fun SlideModeView(slideMode: Settings.SlideMode, onSet: (String, Any) -> Unit){

    CheckItem(
        Icons.Outlined.AutoAwesomeMotion,
        SlideEffect.fromValue(slideMode.effect).toPairWithResString(),
        stringResource(Res.string.slide_effect),
        listOf(
            SlideEffect.Default.toPairWithResString(),
            SlideEffect.Cube.toPairWithResString(),
            SlideEffect.Reveal.toPairWithResString(),
            SlideEffect.Flip.toPairWithResString(),
            //            SlideEffect.Carousel.toPairWithResString()
        ),
        onCheck = {
            onSet(SlideEffect.key, it.first)
        }
    )

    CheckItem(
        if (slideMode.displayMode == DisplayMode.FitScreen.value) Icons.Outlined.FitScreen else Icons.Outlined.FullscreenExit,
        DisplayMode.fromValue(slideMode.displayMode).toPairWithResString(),
        stringResource(Res.string.display_mode),
        listOf(DisplayMode.FitScreen.toPairWithResString(), DisplayMode.CenterCrop.toPairWithResString()),
        onCheck = {
            onSet(DisplayMode.key, it.first)
        }
    )

    if (slideMode.effect == SlideEffect.Default.value || slideMode.effect == SlideEffect.Flip.value) {
        CheckItem(
            if (slideMode.orientation == Orientation.Horizontal.value) Icons.Outlined.WebStories else WebStoriesRotateLeft,
            Orientation.fromValue(slideMode.orientation).toPairWithResString(),
            stringResource(Res.string.orientation),
            listOf(Orientation.Horizontal.toPairWithResString(), Orientation.Vertical.toPairWithResString()),
            onCheck = {
                onSet(Orientation.key, it.first)
            }
        )
    }

    SwitchItem(
        Icons.Outlined.ModelTraining,
        check = slideMode.showTimeProgressIndicator,
        desc = stringResource(Res.string.show_time_progress_indicator),
        onCheck = {
            onSet(ShowTimeProgressIndicator.key, it)
        }
    )

//    SwitchItem(
//        Icons.Outlined.Ballot,
//        check = slideMode.showContentMetaInfo,
//        desc = stringResource(R.string.show_content_meta_info),
//        onCheck = {
//            onSet(ShowContentMetaInfo.key, it)
//        }
//    )
    val secondRange = 5f .. 60f
    val minuteRange = 1f .. 15f

    SlideItem(
        Icons.Outlined.Timer,
        desc = stringResource(Res.string.auto_play),
        value = if (slideMode.intervalTime.toInt() == 0) {
            if (slideMode.intervalTimeUnit == 0) secondRange.start.toInt() else minuteRange.start.toInt()
        } else if ((slideMode.intervalTimeUnit == 0 && slideMode.intervalTime.toFloat() !in secondRange) || (slideMode.intervalTimeUnit == 1 && slideMode.intervalTime.toFloat() !in minuteRange))
            if (slideMode.intervalTimeUnit == 0) secondRange.start.toInt() else minuteRange.start.toInt()
        else
            slideMode.intervalTime,
        range = if (slideMode.intervalTimeUnit == 0) secondRange else minuteRange,
        step = if (slideMode.intervalTimeUnit == 0) ((secondRange.endInclusive - secondRange.start) / 5 - 1).toInt() else (minuteRange.endInclusive - minuteRange.start - 1).toInt(),
        unit = if (slideMode.intervalTimeUnit == 0) " s" else " m",
        onValueChanged = {
            onSet(AutoPlayDuration.key, it)
        }
    )

    CheckItem(
        Icons.Outlined.HistoryToggleOff,
        IntervalTimeUnit.fromValue(slideMode.intervalTimeUnit).toPairWithResString(),
        stringResource(Res.string.interval_time_unit),
        listOf(IntervalTimeUnit.S.toPairWithResString(), IntervalTimeUnit.M.toPairWithResString()),
        onCheck = {
            onSet(IntervalTimeUnit.key, it.first)
        }
    )

    CheckItem(
        Icons.AutoMirrored.Outlined.Sort,
        SortRule.fromValue(slideMode.sortRule).toPair(),
        stringResource(Res.string.sort_rule),
        listOf(SortRule.Random.toPair(), SortRule.NameAsc.toPair(), SortRule.NameDesc.toPair(), SortRule.DateAsc.toPair(), SortRule.DateDesc.toPair()),
        onCheck = {
            onSet(SortRule.key, it.first)
        }
    )


}