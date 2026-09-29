package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Renders the QuoteForge Brand Logo Mark matching the reference design:
 * Emerald teal rounded container (#006D5B), white document sheet with 3 lines,
 * and a warm architectural golden ochre folded ribbon (#DAA574) forming a capital "Q".
 */
@Composable
fun QuoteForgeLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    elevation: Dp = 2.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(elevation, shape = RoundedCornerShape(size * 0.28f))
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF035E4E), Color(0xFF01463A))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height

            // 0. Soft mint accent crescent/ring at top-right corner matching reference image
            drawCircle(
                color = Color(0x3882D3BE),
                center = Offset(w * 0.82f, h * 0.16f),
                radius = w * 0.09f
            )

            // 1. Document Sheet (White with rounded corners, folded top-right, and Q tail notch)
            val docLeft = w * 0.25f
            val docTop = h * 0.23f
            val docWidth = w * 0.46f
            val docHeight = h * 0.50f
            val cornerR = w * 0.10f
            val foldSize = docWidth * 0.35f

            // Shadow under document sheet
            drawRoundRect(
                color = Color(0x2E001F18),
                topLeft = Offset(docLeft + w * 0.015f, docTop + h * 0.02f),
                size = Size(docWidth, docHeight),
                cornerRadius = CornerRadius(cornerR, cornerR)
            )

            // Document Body Path with top-right fold cut
            val docPath = Path().apply {
                // Start top-left after corner radius
                moveTo(docLeft + cornerR, docTop)
                // Top edge towards fold
                lineTo(docLeft + docWidth - foldSize, docTop)
                // Diagonal fold cutout
                lineTo(docLeft + docWidth, docTop + foldSize)
                // Right edge
                lineTo(docLeft + docWidth, docTop + docHeight - cornerR)
                // Bottom-right corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        docLeft + docWidth - cornerR * 2,
                        docTop + docHeight - cornerR * 2,
                        docLeft + docWidth,
                        docTop + docHeight
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Bottom edge
                lineTo(docLeft + cornerR, docTop + docHeight)
                // Bottom-left corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        docLeft,
                        docTop + docHeight - cornerR * 2,
                        docLeft + cornerR * 2,
                        docTop + docHeight
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                // Left edge
                lineTo(docLeft, docTop + cornerR)
                // Top-left corner
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        docLeft,
                        docTop,
                        docLeft + cornerR * 2,
                        docTop + cornerR * 2
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(docPath, Color.White)

            // 2. Folded Top-Right Corner (Caramel/Gold Flap)
            val foldFlap = Path().apply {
                moveTo(docLeft + docWidth - foldSize, docTop)
                lineTo(docLeft + docWidth - foldSize, docTop + foldSize)
                lineTo(docLeft + docWidth, docTop + foldSize)
                close()
            }
            // Shadow behind fold flap
            drawPath(
                Path().apply {
                    moveTo(docLeft + docWidth - foldSize - w * 0.01f, docTop + foldSize)
                    lineTo(docLeft + docWidth - foldSize, docTop + foldSize + h * 0.02f)
                    lineTo(docLeft + docWidth, docTop + foldSize + h * 0.015f)
                    close()
                },
                Color(0x2B002B21)
            )
            drawPath(foldFlap, Color(0xFFC7925B))

            // Highlight crease on fold flap
            drawLine(
                color = Color(0xFFE2B482),
                start = Offset(docLeft + docWidth - foldSize, docTop),
                end = Offset(docLeft + docWidth - foldSize, docTop + foldSize),
                strokeWidth = w * 0.012f
            )

            // 3. Three Horizontal Dark Pine Bars inside document
            val lineThickness = w * 0.048f
            val darkPine = Color(0xFF014539)
            val lineLeft = docLeft + docWidth * 0.18f

            // Top line (medium length)
            drawLine(
                color = darkPine,
                start = Offset(lineLeft, docTop + docHeight * 0.32f),
                end = Offset(docLeft + docWidth * 0.54f, docTop + docHeight * 0.32f),
                strokeWidth = lineThickness,
                cap = StrokeCap.Round
            )
            // Middle line (longest)
            drawLine(
                color = darkPine,
                start = Offset(lineLeft, docTop + docHeight * 0.50f),
                end = Offset(docLeft + docWidth * 0.70f, docTop + docHeight * 0.50f),
                strokeWidth = lineThickness,
                cap = StrokeCap.Round
            )
            // Bottom line (medium length)
            drawLine(
                color = darkPine,
                start = Offset(lineLeft, docTop + docHeight * 0.68f),
                end = Offset(docLeft + docWidth * 0.56f, docTop + docHeight * 0.68f),
                strokeWidth = lineThickness,
                cap = StrokeCap.Round
            )

            // 4. Diagonal Caramel/Gold "Q" Tail
            // Extends diagonally down and right through the bottom-right corner of the document
            val ribbonTail = Path().apply {
                moveTo(docLeft + docWidth * 0.40f, docTop + docHeight * 0.84f)
                lineTo(docLeft + docWidth * 0.90f, docTop + docHeight * 0.84f)
                lineTo(w * 0.82f, h * 0.87f)
                lineTo(w * 0.62f, h * 0.96f)
                lineTo(docLeft + docWidth * 0.36f, docTop + docHeight * 0.88f)
                close()
            }
            // Tail shadow
            drawPath(
                Path().apply {
                    moveTo(docLeft + docWidth * 0.38f, docTop + docHeight * 0.86f)
                    lineTo(w * 0.83f, h * 0.88f)
                    lineTo(w * 0.79f, h * 0.91f)
                    lineTo(docLeft + docWidth * 0.35f, docTop + docHeight * 0.90f)
                    close()
                },
                Color(0x3500241C)
            )

            // The main diagonal caramel gold bar
            val ribbonMain = Path().apply {
                moveTo(docLeft + docWidth * 0.42f, docTop + docHeight * 0.78f)
                lineTo(docLeft + docWidth * 0.92f, docTop + docHeight * 0.82f)
                lineTo(w * 0.81f, h * 0.86f)
                lineTo(w * 0.59f, h * 0.95f)
                close()
            }
            drawPath(ribbonMain, Color(0xFFC7925B))

            // White architectural notch separating document from ribbon
            drawLine(
                color = Color.White,
                start = Offset(docLeft + docWidth * 0.85f, docTop + docHeight * 0.72f),
                end = Offset(w * 0.84f, h * 0.79f),
                strokeWidth = w * 0.024f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Complete QuoteForge Brand Badge with Typography & Tagline
 */
@Composable
fun QuoteForgeHeaderBadge(
    modifier: Modifier = Modifier,
    logoSize: Dp = 44.dp
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuoteForgeLogoMark(size = logoSize)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Quote",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Forge",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = (-0.5).sp
                )
            }
            Text(
                text = "PLAN • PRICE • PROPOSE • CLOSE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * QuoteForge App Thumbnail & Hero Card matching the reference image:
 * "Quotations That Build Tomorrow. PLAN • PRICE • PROPOSE • CLOSE"
 * With 4 Feature Pillars:
 * 1. Create Professional Quotations
 * 2. Work Together
 * 3. Sync with Drive
 * 4. Grow Your Business
 */
@Composable
fun QuoteForgeAppThumbnailCard(
    modifier: Modifier = Modifier,
    onFeatureClick: ((String) -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("quote_forge_hero_thumbnail"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF004D40)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Logo, Branding & Tagline
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuoteForgeLogoMark(size = 48.dp)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quote",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Forge",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color(0xFF7DD3C0)
                            )
                        }
                        Text(
                            text = "PLAN • PRICE • PROPOSE • CLOSE",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDAA574),
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Vertical Divider + Motto
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                    Text(
                        text = "Quotations\nThat Build\nTomorrow.",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 14.sp
                    )
                }
            }

            // 4 Pillars from the reference design
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeaturePill(
                    icon = Icons.Outlined.Description,
                    title = "Professional\nQuotations",
                    iconTint = Color(0xFF006D5B),
                    iconBg = Color(0xFFD2F4EB),
                    onClick = { onFeatureClick?.invoke("quotations") }
                )
                FeaturePill(
                    icon = Icons.Outlined.Group,
                    title = "Work\nTogether",
                    iconTint = Color(0xFF81561C),
                    iconBg = Color(0xFFFBE4C6),
                    onClick = { onFeatureClick?.invoke("team") }
                )
                FeaturePill(
                    icon = Icons.Outlined.CloudQueue,
                    title = "Sync with\nDrive",
                    iconTint = Color(0xFF1E3A8A),
                    iconBg = Color(0xFFDBEAFE),
                    onClick = { onFeatureClick?.invoke("drive") }
                )
                FeaturePill(
                    icon = Icons.Outlined.TrendingUp,
                    title = "Grow Your\nBusiness",
                    iconTint = Color(0xFF7E22CE),
                    iconBg = Color(0xFFF3E8FF),
                    onClick = { onFeatureClick?.invoke("analytics") }
                )
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: ImageVector,
    title: String,
    iconTint: Color,
    iconBg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
