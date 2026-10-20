package com.example.foodcatalog

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

data class Macros(
    val calories: Int,
    val proteins: Double,
    val fats: Double,
    val carbs: Double
)

data class Recipe(
    val title: String,
    val prepTimeMinutes: Int,
    val description: String
)

data class Product(
    val id: String,
    val name: String,
    val category: String,
    val macros: Macros,
    val pairings: List<String>,
    val recipes: List<Recipe>
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FoodCatalogApp()
                }
            }
        }
    }
}

fun loadCatalogFromAssets(context: Context): List<Product> {
    return try {
        val jsonStr = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        val array = JSONArray(jsonStr)
        val list = mutableListOf<Product>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val pairingsArr = obj.getJSONArray("pairings")
            val pairings = (0 until pairingsArr.length()).map { pairingsArr.getString(it) }

            val recipesArr = obj.getJSONArray("recipes")
            val recipes = (0 until recipesArr.length()).map { idx ->
                val rObj = recipesArr.getJSONObject(idx)
                Recipe(
                    title = rObj.getString("title"),
                    prepTimeMinutes = rObj.getInt("prepTimeMinutes"),
                    description = rObj.getString("description")
                )
            }

            list.add(
                Product(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    category = obj.getString("category"),
                    macros = Macros(
                        calories = obj.getInt("calories"),
                        proteins = obj.getDouble("proteins"),
                        fats = obj.getDouble("fats"),
                        carbs = obj.getDouble("carbs")
                    ),
                    pairings = pairings,
                    recipes = recipes
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun FoodCatalogApp() {
    val context = LocalContext.current
    val products = remember { loadCatalogFromAssets(context) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Все") }

    val categories = listOf("Все", "Ягоды", "Фрукты", "Овощи", "Мясо", "Рыба")

    BackHandler(enabled = selectedProduct != null) {
        selectedProduct = null
    }

    if (selectedProduct != null) {
        ProductDetailScreen(
            product = selectedProduct!!,
            onBack = { selectedProduct = null },
            onPairingClick = { pairing ->
                selectedProduct = null
                selectedCategory = "Все"
                searchQuery = pairing
            }
        )
    } else {
        CatalogListScreen(
            products = products,
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelect = { selectedCategory = it },
            searchQuery = searchQuery,
            onSearchChange = { searchQuery = it },
            onProductClick = { selectedProduct = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogListScreen(
    products: List<Product>,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onProductClick: (Product) -> Unit
) {
    val filtered = products.filter { product ->
        val matchesCategory = selectedCategory == "Все" || product.category.equals(selectedCategory, ignoreCase = true)
        val q = searchQuery.trim()
        val matchesSearch = q.isEmpty() ||
            product.name.contains(q, ignoreCase = true) ||
            product.pairings.any { it.contains(q, ignoreCase = true) } ||
            product.recipes.any { it.title.contains(q, ignoreCase = true) }
        matchesCategory && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Каталог продуктов и рецептов", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск продукта, сочетания или рецепта...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { onCategorySelect(category) },
                        label = { Text(category) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtered) { product ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductClick(product) }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = product.category,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${product.macros.calories} ккал • Б: ${product.macros.proteins} г • Ж: ${product.macros.fats} г • У: ${product.macros.carbs} г",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Сочетания: ${product.pairings.take(4).joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    onPairingClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    text = product.category.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Пищевая ценность на 100 г",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MacroItem("Ккал", "${product.macros.calories}")
                            MacroItem("Белки", "${product.macros.proteins} г")
                            MacroItem("Жиры", "${product.macros.fats} г")
                            MacroItem("Углеводы", "${product.macros.carbs} г")
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        text = "Популярные сочетания (нажми для поиска)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        product.pairings.forEach { pairing ->
                            SuggestionChip(
                                onClick = { onPairingClick(pairing) },
                                label = { Text(pairing) }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Рецепты (${product.recipes.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(product.recipes) { recipe ->
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recipe.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${recipe.prepTimeMinutes} мин",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = recipe.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MacroItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
