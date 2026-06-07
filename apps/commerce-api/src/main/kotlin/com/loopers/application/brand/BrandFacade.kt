package com.loopers.application.brand

import com.loopers.domain.brand.BrandService
import org.springframework.stereotype.Component

@Component
class BrandFacade(
    private val brandService: BrandService,
) {
    fun getById(id: Long): BrandInfo = BrandInfo.from(brandService.getById(id))
}
