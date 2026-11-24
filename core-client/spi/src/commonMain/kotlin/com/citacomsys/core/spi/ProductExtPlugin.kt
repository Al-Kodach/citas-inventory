package com.citacomsys.core.spi

import com.citacomsys.types.ProductType
import com.citacomsys.core.domain.repository.SearchResult
import com.citacomsys.types.ProductModule
import org.koin.core.Koin

/**
 * Interface for industrial product extensions (e.g., Pharmacy, Retail).
 */
interface ProductExtPlugin {
    val id: ProductType
    
    /**
     * Map of modules provided by this product. E.g. Patient service
     */
    val manifest: Map<ProductModule, PlatformFeature>

    fun onActivate(koin: Koin)
    fun onDeactivate(koin: Koin)

    /**
     * Optional handler for global search results.
     */
    fun onSearchResultClick(result: SearchResult, navigator: Navigator) {}
}

/**
 * Global registry for available product extensions.
 */
object ProductRegistry {
    private val _plugins = mutableMapOf<ProductType, ProductExtPlugin>()
    val plugins: Map<ProductType, ProductExtPlugin> get() = _plugins

    fun register(plugin: ProductExtPlugin) {
        _plugins[plugin.id] = plugin
    }

    fun get(id: ProductType): ProductExtPlugin? = _plugins.values.firstOrNull { it.id == id }
}
