package com.example.solesense.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.solesense.data.repository.ProductRepository
import com.example.solesense.model.catalog.Brand
import com.example.solesense.model.catalog.ProductModel
import com.example.solesense.repository.BrandRepository
import com.example.solesense.utils.NetworkConnectivityObserver
import com.example.solesense.utils.toProductModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Trạng thái Giao diện Trang chủ
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val brands: List<Brand> = emptyList(),
        val trendingProducts: List<ProductModel>,
        val newArrivals: List<ProductModel>,
        val recommendedProducts: List<ProductModel>
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: ProductRepository = ProductRepository(),
    private val brandRepository: BrandRepository = BrandRepository()
) : AndroidViewModel(application) {

    private val connectivityObserver = NetworkConnectivityObserver(application)
    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetchHomeData()
    }

    fun fetchHomeData(isForceRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isForceRefresh) {
                _isRefreshing.value = true
            } else if (_uiState.value !is HomeUiState.Success) {
                _uiState.value = HomeUiState.Loading
            }

            val productsDeferred = async { repository.getProducts(isForceRefresh = isForceRefresh) }
            val brandsDeferred = async { brandRepository.getActiveBrands() }

            val productsResult = productsDeferred.await()
            val brandsResult = brandsDeferred.await()

            if (productsResult.isSuccess) {
                val rawProducts = productsResult.getOrNull().orEmpty()
                val allProductModels = rawProducts.map { it.toProductModel() }
                val brands = brandsResult.getOrNull().orEmpty()

                _uiState.value = HomeUiState.Success(
                    brands = brands,
                    trendingProducts = allProductModels.sortedByDescending { it.soldCount },
                    newArrivals = allProductModels,
                    recommendedProducts = allProductModels
                )
            } else {
                val exception = productsResult.exceptionOrNull()
                // Nếu đang refresh mà lỗi (ví dụ không có mạng), giữ nguyên data cũ và chỉ tắt refreshing
                if (!isForceRefresh || _uiState.value !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(
                        exception?.localizedMessage ?: "Không thể kết nối với máy chủ"
                    )
                }
            }

            _isRefreshing.value = false
        }
    }
}
