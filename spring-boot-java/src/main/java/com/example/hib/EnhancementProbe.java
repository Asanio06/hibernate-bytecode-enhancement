package com.example.hib;

import org.hibernate.Session;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

/**
 * Proves Hibernate 7 bytecode enhancement at runtime, Spring style:
 *  1. Product implements SelfDirtinessTracker (enhancer injected it).
 *  2. Lazy description loads on access.
 *  3. Unchanged save (no mutation) -> 0 UPDATE.
 *  4. Mutation -> 1 UPDATE.
 */
@Component
public class EnhancementProbe implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EnhancementProbe.class);

    private final ProductRepository repo;
    private final EntityManager em;

    public EnhancementProbe(ProductRepository repo, EntityManager em) {
        this.repo = repo;
        this.em = em;
    }

    @Override
    @Transactional
    public void run(String... args) {
        boolean isEnhanced = org.hibernate.engine.spi.SelfDirtinessTracker.class
                .isAssignableFrom(Product.class);
        log.info("== Product.class enhanced (SelfDirtinessTracker) == {}", isEnhanced);

        Statistics stats = em.unwrap(Session.class).getSessionFactory().getStatistics();
        stats.setStatisticsEnabled(true);

        Product p = repo.save(new Product("Coffee", "lazy description of coffee, under 255 chars"));
        Long id = p.getId();

        em.clear();
        Product loaded = repo.findById(id).orElseThrow();
        log.info("== lazy description BEFORE access: loaded base group only, name={}", loaded.getName());
        // trigger the lazy group
        String desc = loaded.getDescription();
        log.info("== lazy description AFTER access, length={}", desc.length());

        // no mutation -> enhanced dirty tracking must detect clean and skip UPDATE
        stats.clear();
        repo.saveAndFlush(loaded);
        log.info("== unchanged save: entity UPDATE = {} (expect 0)", stats.getEntityUpdateCount());

        // mutate -> exactly one UPDATE
        stats.clear();
        loaded.setName("Coffee XL");
        repo.saveAndFlush(loaded);
        log.info("== changed name: entity UPDATE = {} (expect 1)", stats.getEntityUpdateCount());

        log.info("ALL SPRING BOOT JAVA CHECKS PASSED");
    }
}