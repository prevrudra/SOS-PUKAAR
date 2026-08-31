package com.pukaar.app.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.TextPrimary

/** One full-screen page of an illustrated walkthrough. */
data class ImagePage(
    @DrawableRes val image: Int,
    @StringRes val description: Int
)

/**
 * An illustrated walkthrough: one artwork per page, swiped left to right.
 *
 * Full-bleed rather than inside [PukaarScreen] — each page is a whole screen
 * mock-up, so it gets the entire viewport while the back control and the page
 * dots float over it. Used by Mock Drill and How Pukaar Works.
 */
@Composable
fun ImagePagerScreen(
    pages: List<ImagePage>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            val page = pages[index]
            Image(
                painter = painterResource(page.image),
                contentDescription = stringResource(page.description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        CircularBackButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
        )

        PageIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        )
    }
}

/**
 * Light dots over the artwork so the current page is readable on the dark mock-ups.
 * Kept small: these walkthroughs run to a dozen pages or more, and the whole row
 * has to fit a narrow screen.
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { page ->
            val selected = page == currentPage
            Box(
                modifier = Modifier
                    .size(if (selected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) TextPrimary else TextPrimary.copy(alpha = 0.35f)
                    )
            )
        }
    }
}
