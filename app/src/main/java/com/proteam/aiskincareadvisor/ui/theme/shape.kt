package com.proteam.aiskincareadvisor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),   // 小组件（Tag、Chip）
    medium = RoundedCornerShape(18.dp),  // 卡片、按钮
    large = RoundedCornerShape(28.dp)    // 大块区域
)