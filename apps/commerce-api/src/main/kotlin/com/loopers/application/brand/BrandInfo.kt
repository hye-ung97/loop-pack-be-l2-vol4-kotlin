package com.loopers.application.brand

import com.loopers.domain.brand.BrandModel

data class BrandInfo(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(brand: BrandModel): BrandInfo = BrandInfo(id = brand.id, name = brand.name)
    }
}
