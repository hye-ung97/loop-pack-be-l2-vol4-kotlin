package com.loopers.domain.product

import org.springframework.data.domain.Sort

enum class ProductSort(val sort: Sort) {
    LATEST(Sort.by(Sort.Direction.DESC, "createdAt")),
    PRICE_ASC(Sort.by(Sort.Direction.ASC, "price")),
    LIKES_DESC(Sort.by(Sort.Direction.DESC, "likeCount")),
    ;

    companion object {
        fun from(value: String?): ProductSort = when (value?.lowercase()) {
            "price_asc" -> PRICE_ASC
            "likes_desc" -> LIKES_DESC
            else -> LATEST
        }
    }
}
