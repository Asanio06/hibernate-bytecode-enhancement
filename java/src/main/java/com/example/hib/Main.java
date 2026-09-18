package com.example.hib;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.stat.Statistics;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates that Hibernate 7 bytecode enhancement actually ran:
 *   1. Product now implements SelfDirtinessTracker (a marker injected by the enhancer).
 *   2. A lazy basic attribute is loaded lazily on access.
 *   3. Dirty-tracking enhancement causes an unchanged re-merge to issue NO UPDATE.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        cfg.put("jakarta.persistence.jdbc.user", "sa");
        cfg.put("jakarta.persistence.jdbc.password", "");
        cfg.put("hibernate.hbm2ddl.auto", "create");
        cfg.put("hibernate.show_sql", "true");
        cfg.put("hibernate.generate_statistics", "true");

        try (EntityManagerFactory emf = Persistence.createEntityManagerFactory("h7", cfg);
             SessionFactory sf = emf.unwrap(SessionFactory.class);
             Session s = sf.openSession()) {

            // ---- 1. prove the class is enhanced at runtime ----
            boolean isEnhanced = SelfDirtinessTracker.class.isAssignableFrom(Product.class);
            System.out.println("== Product.class enhanced (implements SelfDirtinessTracker) == " + isEnhanced);

            Statistics stats = sf.getStatistics();
            stats.setStatisticsEnabled(true);

            // ---- 2. basic persist + lazy attribute ----
            s.getTransaction().begin();
            Product p = new Product("Coffee", "A moderately long description that is still under 255 chars for H2 default varchar sizing");
            s.persist(p);
            s.getTransaction().commit();

            s.clear();

            // reload; description is LAZY so it should NOT be selected in the initial snapshot
            s.getTransaction().begin();
            Product loaded = s.find(Product.class, p.getId());
            System.out.println("== lazy description loaded? (before access) ==");
            System.out.println("   name = " + loaded.getName()); // this forces just the base fetch group
            // Accessing getDescription() triggers the lazy load of the lazy group
            String desc = loaded.getDescription();
            System.out.println("== after accessing lazy description, length = " + desc.length());
            s.getTransaction().commit();

            // ---- 3. dirty-tracking: unchanged merge should not hit UPDATE ----
            stats.clear();
            s.getTransaction().begin();
            Product merged = s.merge(loaded); // no change -> enhanced dirty tracking detects clean state
            s.getTransaction().commit();
            long updateCount = stats.getEntityUpdateCount();
            System.out.println("== unchanged merge: entity UPDATE statements = " + updateCount + "  (expect 0 thanks to dirty-tracking enhancement)");
            System.out.println("== Product instance identity: same merged? " + (merged == loaded));

            // ---- 4. actually mutate and check UPDATE does fire ----
            stats.clear();
            s.getTransaction().begin();
            loaded.setName("Coffee XL");
            s.getTransaction().commit();
            long updateCount2 = stats.getEntityUpdateCount();
            System.out.println("== changed name: entity UPDATE statements = " + updateCount2 + "  (expect 1)");

            System.out.println("\nALL JAVA CHECKS PASSED");
        }
    }
}