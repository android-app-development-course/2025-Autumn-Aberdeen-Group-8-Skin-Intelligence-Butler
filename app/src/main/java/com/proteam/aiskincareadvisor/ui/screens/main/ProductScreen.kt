package com.proteam.aiskincareadvisor.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.proteam.aiskincareadvisor.R
import com.proteam.aiskincareadvisor.data.model.Product
import com.proteam.aiskincareadvisor.data.viewmodel.SkinHistoryViewModel
import com.proteam.aiskincareadvisor.ui.components.ProductDetailDialog
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen() {
    // State
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf("All") }

    val skinViewModel: SkinHistoryViewModel = viewModel()
    val latestResult by skinViewModel.latestResult.collectAsState()
    var showDefaultRecommendations by remember { mutableStateOf(false) }

    // 默认推荐商品列表（当没有皮肤检测结果时显示）
    val defaultRecommendedProducts = remember {
        listOf(
            Product(
                id = "default_1",
                name = "CeraVe Hydrating Cleanser",
                category = "Cleanser",
                price = "$16.99",
                imageUrl = "https://img.alicdn.com/i2/2970133769/O1CN01ZIYgx31diFkmuxoAj_!!2970133769.jpg",
                skinTypes = listOf("Dry", "Sensitive", "Normal"),
                description = "Gentle, non-foaming cleanser that hydrates and restores the protective skin barrier"
            ),
            Product(
                id = "default_2",
                name = "La Roche-Posay Toleriane Moisturizer",
                category = "Moisturizer",
                price = "$29.99",
                imageUrl = "https://img.alicdn.com/i3/307598068/O1CN01xDxKb729TCC9s7X1Q_!!307598068.jpg",
                skinTypes = listOf("Sensitive", "Normal", "Combination"),
                description = "Soothing moisturizer for sensitive skin with prebiotic thermal water"
            ),
            Product(
                id = "default_3",
                name = "The Ordinary Niacinamide 10% + Zinc 1%",
                category = "Serum",
                price = "$5.90",
                imageUrl = "https://cbu01.alicdn.com/img/ibank/O1CN01AAYb291xjtOMOHqax_!!2216678536480-0-cib.jpg",
                skinTypes = listOf("Oily", "Combination", "Acne-Prone"),
                description = "High-strength vitamin and mineral blemish formula"
            ),
            Product(
                id = "default_4",
                name = "Neutrogena Hydro Boost Sunscreen",
                category = "Sunscreen",
                price = "$14.97",
                imageUrl = "https://ts4.tc.mm.bing.net/th/id/OIP-C.Uhq6LQ1dGuL0I74OYIJwJAHaHa?cb=ucfimg2ucfimg=1&rs=1&pid=ImgDetMain&o=7&rm=3",
                skinTypes = listOf("All", "Dry", "Normal"),
                description = "Water gel formula with hyaluronic acid, SPF 50"
            ),
            Product(
                id = "default_5",
                name = "COSRX Advanced Snail 96 Mucin Power Essence",
                category = "Essence",
                price = "$25.00",
                imageUrl = "https://cbu01.alicdn.com/img/ibank/O1CN01eW0yak1wT7K05ZBNG_!!2217660616308-0-cib.jpg",
                skinTypes = listOf("All", "Dry", "Sensitive"),
                description = "Snail secretion filtrate for hydrated and repaired skin"
            ),
            Product(
                id = "default_6",
                name = "Kiehl's Ultra Facial Cream",
                category = "Moisturizer",
                price = "$36.00",
                imageUrl = "https://pic3.zhimg.com/v2-16f29a0e0de543e3ba038a1ffdce1b12_b.jpg",
                skinTypes = listOf("All", "Dry", "Normal"),
                description = "24-hour daily hydrating cream for all skin types"
            )
        )
    }

    // 根据是否有皮肤检测结果决定显示的内容
    val displayProducts = remember(latestResult, products) {
        if (latestResult?.recommendedProductIds?.isNotEmpty() == true) {
            showDefaultRecommendations = false
            products
        } else {
            showDefaultRecommendations = true
            defaultRecommendedProducts
        }
    }

    // Calculate categories from products
    val allCategories = remember(displayProducts) {
        if (showDefaultRecommendations) {
            listOf("All")
        } else {
            listOf("All") + displayProducts.map { it.category }.distinct().sorted()
        }
    }

    // Filter products based on selected category
    val filteredProducts = remember(selectedCategory, displayProducts) {
        if (selectedCategory == "All") {
            displayProducts
        } else {
            displayProducts.filter { it.category == selectedCategory }
        }
    }

    // Fetch products from Firestore (only if we have skin analysis results)
    LaunchedEffect(showDefaultRecommendations) {
        if (!showDefaultRecommendations) {
            try {
                val db = FirebaseFirestore.getInstance()
                val productsCollection = db.collection("products")
                val snapshot = productsCollection.get().await()

                val productsList = snapshot.documents.mapNotNull { document ->
                    document.toObject(Product::class.java)?.copy(id = document.id)
                }

                products = productsList
                isLoading = false
            } catch (e: Exception) {
                error = e.message
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Skincare Products",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { /* Search action */ }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    LoadingState()
                }
                error != null -> {
                    ErrorState(error!!) { isLoading = true; error = null }
                }
                else -> {
                    // 修复：使用单个LazyColumn而不是嵌套滚动容器
                    if (showDefaultRecommendations) {
                        DefaultRecommendationsSection(
                            products = displayProducts,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        PersonalizedRecommendationsSection(
                            products = displayProducts,
                            selectedCategory = selectedCategory,
                            allCategories = allCategories,
                            filteredProducts = filteredProducts,
                            onCategorySelected = { selectedCategory = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading products...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ErrorState(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.placeholder),
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Something went wrong",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .width(120.dp)
                    .height(48.dp)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun CategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory

            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = {
                    Text(
                        text = category,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun ProductGrid(
    products: List<Product>
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products.size) { index ->
            ProductCard(
                product = products[index]
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product
) {
    var isFavorite by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailDialog = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // 增加图片高度
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .error(R.drawable.placeholder)
                        .placeholder(R.drawable.placeholder)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Favorite button
                IconButton(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    modifier = Modifier.height(40.dp) // 固定标题高度
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (product.skinTypes.isNotEmpty()) {
                    Text(
                        text = product.skinTypes.joinToString(" • "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.height(16.dp) // 固定皮肤类型高度
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product.price,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FilledTonalButton(
                        onClick = { /* Buy action */ },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Text(
                            text = "Buy",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    // Show product detail dialog when clicked
    if (showDetailDialog) {
        ProductDetailDialog(
            product = product,
            primaryColor = MaterialTheme.colorScheme.primary,
            textPrimaryColor = MaterialTheme.colorScheme.onSurface,
            textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismiss = { showDetailDialog = false }
        )
    }
}

@Composable
fun DefaultRecommendationsSection(
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 提示信息卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .padding(bottom = 18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Get Personalized Recommendations",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Finish a skin analysis to get product suggestions tailored to your own skin type",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 推荐产品标题
        Text(
            text = "Popular Products for All Skin Types",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 修复：使用单个LazyVerticalGrid作为主要滚动容器
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(products.size) { index ->
                ProductCard(
                    product = products[index]
                )
            }
        }
    }
}

@Composable
fun PersonalizedRecommendationsSection(
    products: List<Product>,
    selectedCategory: String,
    allCategories: List<String>,
    filteredProducts: List<Product>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
    ) {
        // 推荐产品标题
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recommended For You",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            TextButton(onClick = { /* View all */ }) {
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Category Tabs
        CategoryTabs(
            categories = allCategories,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        // 产品数量统计
        Text(
            text = "${filteredProducts.size} products found",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
        )

        // Product Grid - 修复：使用单个LazyVerticalGrid作为主要滚动容器
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            items(filteredProducts.size) { index ->
                ProductCard(
                    product = filteredProducts[index]
                )
            }
        }
    }
}

@Composable
fun RecommendedProductCard(product: Product) {
    var showDetailDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { showDetailDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .error(R.drawable.placeholder)
                        .placeholder(R.drawable.placeholder)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (product.skinTypes.isNotEmpty()) {
                    Text(
                        text = product.skinTypes.take(2).joinToString(" • "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = product.price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDetailDialog) {
        ProductDetailDialog(
            product = product,
            primaryColor = MaterialTheme.colorScheme.primary,
            textPrimaryColor = MaterialTheme.colorScheme.onSurface,
            textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onDismiss = { showDetailDialog = false }
        )
    }
}