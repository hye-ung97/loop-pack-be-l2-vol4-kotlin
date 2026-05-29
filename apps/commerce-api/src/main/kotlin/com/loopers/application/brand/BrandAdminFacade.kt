package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BrandAdminFacade(
    private val brandService: BrandService,
    private val productService: ProductService,
) {
    fun create(name: String): BrandInfo = BrandInfo.from(brandService.register(name))

    fun getById(id: Long): BrandInfo = BrandInfo.from(brandService.getById(id))

    fun getAll(pageable: Pageable): Page<BrandInfo> = brandService.getAll(pageable).map(BrandInfo::from)

    fun update(id: Long, name: String) {
        brandService.rename(id, name)
    }

    @Transactional
    fun delete(id: Long) {
        productService.softDeleteAllByBrandId(id)
        brandService.delete(id)
    }
}
