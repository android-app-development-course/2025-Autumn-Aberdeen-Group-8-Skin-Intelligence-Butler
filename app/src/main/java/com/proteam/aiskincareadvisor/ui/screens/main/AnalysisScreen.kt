package com.proteam.aiskincareadvisor.ui.screens.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.proteam.aiskincareadvisor.R
import com.proteam.aiskincareadvisor.data.viewmodel.SkinHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(navController: NavController, onNavigateToAnalysis: () -> Unit) {
    val viewModel: SkinHistoryViewModel = viewModel()
    val latestResult by viewModel.latestResult.collectAsState()

    Scaffold(
        // 可以后续加 topBar 等
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),   // ✅ 用主题背景色
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { LastScanHeader(latestResult?.timestamp) }
            item {
                SkinHealthSummarySection(
                    latestResult?.hydrationLevel,
                    latestResult?.oilLevel,
                    latestResult?.overallCondition
                )
            }
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReanalyzeButton(onReanalyze = onNavigateToAnalysis)
                    ViewRoutineRecommendationsButton { navController.navigate("routine") }
                }
            }
            item {
                DetailedBreakdownSection(
                    latestResult?.skinType,
                    latestResult?.concerns ?: emptyList()
                )
            }
            item { TipsAndInsightsSection(latestResult?.tips ?: emptyList()) }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun LastScanHeader(timestamp: Long?) {
    val formattedTime = timestamp?.let {
        val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.getDefault())
        sdf.format(Date(it))
    } ?: "Không có dữ liệu"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface      // ✅ 卡片背景
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Lần phân tích gần nhất",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface          // ✅ 文字色跟随主题
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedTime,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // ✅ 柔和灰
            )
        }
    }
}

@Composable
fun SkinHealthSummarySection(
    hydration: String?,
    oil: String?,
    condition: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // ✅ 马卡龙卡片底
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TỔNG QUAN LÀN DA",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary             // ✅ 主色标题
            )
            Spacer(modifier = Modifier.height(20.dp))

            // 用 primary / secondary / tertiary 做三段色
            SummaryLine(
                label = "Độ ẩm",
                value = hydration ?: "Không có dữ liệu",
                valueColor = MaterialTheme.colorScheme.tertiary
            )
            SummaryLine(
                label = "Độ dầu",
                value = oil ?: "Không có dữ liệu",
                valueColor = MaterialTheme.colorScheme.secondary
            )
            SummaryLine(
                label = "Tổng thể",
                value = condition ?: "Không có dữ liệu",
                valueColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SummaryLine(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), // ✅ 柔和标签色
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DetailedBreakdownSection(skinType: String?, concerns: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface      // ✅ 统一卡片底
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "CHI TIẾT PHÂN TÍCH",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary           // ✅ 主色标题
            )
            Spacer(modifier = Modifier.height(16.dp))
            skinType?.let {
                BreakdownItem("Loại da", it, R.drawable.ic_lock) // TODO: 更新为合适图标
            }
            if (concerns.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                BreakdownItem(
                    "Vấn đề da",
                    concerns.joinToString(", "),
                    R.drawable.ic_lock
                ) // TODO: 同上
            }
        }
    }
}

@Composable
fun BreakdownItem(label: String, value: String, iconResource: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant        // ✅ 圆形浅底
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconResource),
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,           // ✅ 图标主色
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) // ✅ 次级文字色
            )
        }
    }
}

@Composable
fun TipsAndInsightsSection(tips: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "MẸO & GỢI Ý",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary          // ✅ 主色标题
            )
            Spacer(modifier = Modifier.height(16.dp))

            val displayTips = if (tips.isNotEmpty()) tips else listOf(
                "Luôn dùng kem chống nắng hàng ngày.",
                "Uống đủ 2 lít nước mỗi ngày để giữ ẩm cho da."
            )

            displayTips.forEach {
                TipItem(it)
                if (it != displayTips.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun TipItem(tip: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)      // ✅ 小圆点用主色
                .align(Alignment.CenterVertically)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = tip,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground,        // ✅ 主文字色
            lineHeight = 20.sp
        )
    }
}

@Composable
fun ReanalyzeButton(onReanalyze: () -> Unit) {
    Button(
        onClick = onReanalyze,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,    // ✅ 马卡龙主色按钮
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_camera),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Phân tích lại",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ViewRoutineRecommendationsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), // ✅ 主色描边
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary              // ✅ 图标+文字主色
        ),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_spa),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Xem gợi ý chăm sóc da",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}