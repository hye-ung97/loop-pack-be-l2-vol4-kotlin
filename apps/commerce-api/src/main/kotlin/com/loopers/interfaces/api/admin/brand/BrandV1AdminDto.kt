package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandInfo

class BrandV1AdminDto {
    data class CreateRequest(
        val name: String,
    )

    data class UpdateRequest(
        val name: String,
    )

    data class BrandResponse(
        val id: Long,
        val name: String,
    ) {
        companion object {
            fun from(info: BrandInfo): BrandResponse = BrandResponse(id = info.id, name = info.name)
        }
    }

    data class BrandPageResponse(
        val content: List<BrandResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
