package com.snowify.app.ui.screens.friends
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import com.snowify.app.ui.components.AccentButton
import com.snowify.app.ui.theme.SnowifyTheme
@Composable
fun FriendsScreen() {
    val colors = SnowifyTheme.colors
    Column(modifier = Modifier.fillMaxSize().background(colors.bgBase)) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Friends",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                color = colors.accentDim,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(500.dp),
            ) {
                Text(
                    "BETA",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(Icons.Filled.Group, null, tint = colors.textSubdued, modifier = Modifier.size(72.dp))
                Spacer(Modifier.height(24.dp))
                Text(
                    "Friends feature coming soon",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in to connect with friends and see what they're listening to.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
