package com.loopers.domain.brand

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class BrandModel(
    name: String,
) : BaseEntity() {
    var name: String = name
        protected set

    init {
        requireValidName(name)
    }

    fun rename(newName: String) {
        requireValidName(newName)
        this.name = newName
    }

    private fun requireValidName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 비어있을 수 없습니다.")
        }
    }
}
