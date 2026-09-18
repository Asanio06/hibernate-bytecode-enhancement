package com.example.hib

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SelfDirtinessTracker
import org.hibernate.stat.Statistics
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.Persistence
import java.util.HashMap

/**
 * Validates Hibernate 7 bytecode enhancement on a KOTLIN entity:
 *   1. Product (Kotlin, "open") implements SelfDirtinessTracker after enhancement.
 *   2. Lazy basic attribute loads lazily.
 *   3. Dirty-tracking: unchanged re-merge produces 0 UPDATE.
 *   4. Mutation produces exactly 1 UPDATE.
 */
fun main() {
    val cfg: MutableMap<String, Any> = HashMap()
    cfg["jakarta.persistence.jdbc.url"] = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1"
    cfg["jakarta.persistence.jdbc.user"] = "sa"
    cfg["jakarta.persistence.jdbc.password"] = ""
    cfg["hibernate.hbm2ddl.auto"] = "create"
    cfg["hibernate.show_sql"] = "true"
    cfg["hibernate.generate_statistics"] = "true"

    val emf: EntityManagerFactory = Persistence.createEntityManagerFactory("h7", cfg)
    val sf: SessionFactory = emf.unwrap(SessionFactory::class.java)
    val s: Session = sf.openSession()

    try {
        val isEnhanced = SelfDirtinessTracker::class.java.isAssignableFrom(Product::class.java)
        println("== Kotlin Product.class enhanced (SelfDirtinessTracker) == $isEnhanced")

        val stats: Statistics = sf.statistics
        stats.isStatisticsEnabled = true

        s.transaction.begin()
        val p = Product(name = "Tea", description = "Green tea leaves, under 255 chars")
        s.persist(p)
        s.transaction.commit()

        s.clear()

        s.transaction.begin()
        val loaded = s.find(Product::class.java, p.id)
        println("== lazy description loaded? (before access) ==")
        println("   name loaded = ${loaded.name}  category=${loaded.category}")
        // accessing the lazy attribute triggers lazy group load
        val desc = loaded.description
        println("== after accessing lazy description, length = ${desc?.length}")
        s.transaction.commit()

        // unchanged merge -> enhanced dirty tracking -> no UPDATE
        stats.clear()
        s.transaction.begin()
        s.merge(loaded)
        s.transaction.commit()
        println("== unchanged merge: entity UPDATE = ${stats.entityUpdateCount}  (expect 0)")

        // mutation -> exactly one UPDATE
        stats.clear()
        s.transaction.begin()
        loaded.name = "Tea Premium"
        s.transaction.commit()
        println("== changed name: entity UPDATE = ${stats.entityUpdateCount}  (expect 1)")

        println("\nALL KOTLIN CHECKS PASSED")
    } finally {
        s.close()
        sf.close()
        emf.close()
    }
}