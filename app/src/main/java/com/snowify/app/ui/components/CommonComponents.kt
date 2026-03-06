@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)
package com.snowify.app.ui.components
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.snowify.app.data.model.Song
import com.snowify.app.ui.theme.SnowifyTheme
import kotlinx.coroutines.delay
@Composable
fun SongCard(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLike: (() -> Unit)? = null,
    onArtistClick: (() -> Unit)? = null,
    showLikeButton: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .background(color = Color.Transparent, shape = RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = {
                    pressed = true
                    onClick()
                },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.thumbnailUrl,
            contentDescription = song.title,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (song.artistName.isNotBlank()) {
                Text(
                    text = song.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onArtistClick != null) colors.accent else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = if (onArtistClick != null) Modifier.clickable(onClick = onArtistClick) else Modifier,
                )
            }
        }
        if (showLikeButton && onLike != null) {
            LikeButton(isLiked = song.isLiked, onToggle = onLike)
        }
    }
    LaunchedEffect(pressed) {
        if (pressed) { delay(150); pressed = false }
    }
}
@Composable
fun LikeButton(
    isLiked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (isLiked) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = 0.4f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "like_scale"
    )
    IconButton(
        onClick = onToggle,
        modifier = modifier.scale(scale),
    ) {
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (isLiked) "Unlike" else "Like",
            tint = if (isLiked) colors.accent else colors.textSubdued,
        )
    }
}
@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String,
    onClick: () -> Unit,
    onSubtitleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    Column(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = if (onSubtitleClick != null) colors.accent else colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (onSubtitleClick != null) Modifier.clickable(onClick = onSubtitleClick) else Modifier,
        )
    }
}

@Composable
fun ArtistCard(
    name: String,
    thumbnailUrl: String,
    subscriberCount: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    Column(
        modifier = modifier
            .width(120.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AsyncImage(
            model = thumbnailUrl,
            contentDescription = name,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (subscriberCount.isNotBlank()) {
            Text(
                text = subscriberCount,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun SearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(500.dp),
        color = colors.bgElevated,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = colors.textSubdued,
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Search songs, artists, albums...",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSubdued,
            )
        }
    }
}
@Composable
fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See all",
                    color = colors.accent,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
@Composable
fun EmptyState(
    message: String,
    icon: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = SnowifyTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        icon()
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSubdued,
        )
    }
}
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SnowifyTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = Color.White,
            disabledContainerColor = colors.textSubdued,
        ),
        shape = RoundedCornerShape(500.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold)
    }
}
@Composable
fun SnowifyTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = SnowifyTheme.colors
    TopAppBar(
        title = {
            Text(
                text = title,
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        },
        navigationIcon = if (onBack != null) {
            {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                    )
                }
            }
        } else ({}),
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.bgSurface,
            titleContentColor = colors.textPrimary,
        ),
    )
}
