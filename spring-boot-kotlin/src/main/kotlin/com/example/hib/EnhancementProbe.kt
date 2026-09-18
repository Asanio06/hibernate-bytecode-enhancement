package com.example.hib

import org.hibernate.Session
import org.hibernate.engine.spi.SelfDirtinessTracker
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager

@Component
open class EnhancementProbe(
    private val repo: ProductRepository,
    private val em: EntityManager,
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(EnhancementProbe::class.java)

    @Transactional
    override fun run(vararg args: String) {
        val isEnhanced = SelfDirtinessTracker::class.java.isAssignableFrom(Product::class.java)
        log.info("== Kotlin Product.class enhanced (SelfDirtinessTracker) == {}", isEnhanced)

        val stats = em.unwrap(Session::class.java).sessionFactory.statistics
        stats.isStatisticsEnabled = true

        val p = repo.save(Product(name = "Tea", description = "green tea leaves, under 255 chars"))
        val id = p.id!!

        em.clear()
        val loaded = repo.findById(id).orElseThrow()
        log.info("== lazy description BEFORE access: name={}", loaded.name)
        val len = loaded.description?.length
        log.info("== lazy description AFTER access, length={}", len)

        stats.clear()
        repo.saveAndFlush(loaded)
        log.info("== unchanged save: entity UPDATE = {} (expect 0)", stats.entityUpdateCount)

        stats.clear()
        loaded.name = "Tea Premium"
        repo.saveAndFlush(loaded)
        log.info("== changed name: entity UPDATE = {} (expect 1)", stats.entityUpdateCount)

        log.info("ALL SPRING BOOT KOTLIN CHECKS PASSED")
    }
}