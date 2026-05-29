package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class BrandService(
    private val brandRepository: BrandRepository,
) {
    @Transactional
    fun register(name: String): BrandModel {
        val existing = brandRepository.findByNameIncludingDeleted(name)
        return when {
            existing == null -> brandRepository.save(BrandModel(name = name))
            existing.deletedAt != null -> existing.also { it.restore() }
            else -> throw CoreException(ErrorType.CONFLICT, "이미 사용 중인 브랜드 이름입니다.")
        }
    }

    fun getById(id: Long): BrandModel {
        return brandRepository.findActiveById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 브랜드입니다.")
    }

    fun getAll(pageable: Pageable): Page<BrandModel> = brandRepository.findAllActive(pageable)

    fun findAllActiveByIds(ids: List<Long>): List<BrandModel> = brandRepository.findAllActiveByIds(ids)

    @Transactional
    fun rename(id: Long, newName: String) {
        val brand = getById(id)
        brand.rename(newName)
    }

    @Transactional
    fun delete(id: Long) {
        val brand = brandRepository.findActiveById(id) ?: return
        brand.delete()
    }
}
