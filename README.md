# Hibernate 7+ Bytecode Enhancement — Exemples complets (Java & Kotlin)

Projets Maven **fonctionnels** qui prouvent que le bytecode enhancement de
[Hibernate ORM 7.x](https://hibernate.org/orm/releases/7.0/) marche bien, avec le
nouveau plugin **`org.hibernate.orm:hibernate-maven-plugin`** (réécrit en 7.0).

Quatre variantes, toutes validées à l'exécution :

| Module | Langage | Styling | Config |
|---|---|---|---|
| [`java/`](./java) | Java | CLI (Main) | `persistence.xml` |
| [`kotlin/`](./kotlin) | Kotlin | CLI (Main) | `persistence.xml` |
| [`spring-boot-java/`](./spring-boot-java) | Java | **Spring Boot 4.1** | `application.properties`, **zéro XML** |
| [`spring-boot-kotlin/`](./spring-boot-kotlin) | Kotlin | **Spring Boot 4.1** | `application.properties`, **zéro XML** |

> 💡 **Pour démarrer vite**, prends les versions Spring Boot : pas de `persistence.xml`,
> la config vit dans `src/main/resources/application.properties`, et Hibernate
> 7.4 est géré nativement par Spring Boot 4.1 (aucune version à déclarer).

---

## Ce que ça valide (à l'exécution)

| Vérification | Java | Kotlin |
|---|---|---|
| `Product` implémente `ManagedEntity` + `SelfDirtinessTracker` (enhancement injecté) | ✅ | ✅ |
| Attribut `@Basic(fetch = LAZY)` chargé au premier accès | ✅ | ✅ |
| Re-save **sans modification** → **0 UPDATE** (dirty-tracking enhancement) | ✅ | ✅ |
| Mutation réelle → **1 UPDATE** | ✅ | ✅ |

## Pré-requis

- **JDK 17+** (projets Spring Boot : JDK 21)
- **Maven 3.8+**
- Base H2 embarquée (aucune config requise)

## Lancer — version Spring Boot (recommandée)

```bash
cd spring-boot-java          # ou spring-boot-kotlin
mvn spring-boot:run
```

L'app démarre, exécute `EnhancementProbe` (un `CommandLineRunner`) puis reste en
écoute. Les logs contiennent :

```
== ...Product.class enhanced (implements SelfDirtinessTracker) == true
== lazy description AFTER access, length = NN
== unchanged save: entity UPDATE = 0  (expect 0)
== changed name: entity UPDATE = 1  (expect 1)
ALL SPRING BOOT ... CHECKS PASSED
```

## Lancer — version CLI minimale

```bash
# Java
cd java && mvn package && mvn exec:java -Dexec.mainClass=com.example.hib.Main
# Kotlin
cd kotlin && mvn package && mvn exec:java -Dexec.mainClass=com.example.hib.MainKt
```

---

## Le point clé : le plugin Maven depuis 7.0

Le plugin a changé de coordonnées en 7.0 :

```xml
<plugin>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-maven-plugin</artifactId>
    <version>7.4.9.Final</version>
    <executions>
        <execution>
            <goals>
                <goal>enhance</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> L'ancien `org.hibernate.orm.tooling:hibernate-enhance-maven-plugin` est remplacé.
> ⚠️ Depuis **7.1.5**, `enableLazyInitialization` et `enableDirtyTracking` sont
> repassés à `true` par défaut (une régression 7.0 les avait mis à `false`).
> Sur 7.0.x, active-les explicitement.

### Options dépréciées en 7.1+

- `enableExtendedEnhancement` — utilise plutôt getters/setters + encapsulation
- `enableAssociationManagement` (bidirectionnel auto) — gère les deux côtés à la main
- l'enhancement runtime (`hibernate.enhancer.*`) — préférer l'enhancement au build

### Kotlin : pièges à connaître

1. **Classe `open` + propriétés `var`** — les classes Kotlin sont `final` par défaut,
   l'enhancer ne peut pas tisser dans une classe finale :
   ```kotlin
   @Entity
   open class Product(          // <-- obligatoire : "open"
       var name: String,        // <-- "var", pas "val"
       var description: String?,
   )
   ```
2. **Avec Spring, les `@Component`/`.run()` doivent être `open`** — Spring utilise
   CGLIB pour le proxying (`@Transactional`), et ne peut pas sous-classer une classe finale :
   ```kotlin
   @Component
   open class EnhancementProbe(...) : CommandLineRunner { ... }
   ```

---

## Versions

- Spring Boot **4.1.1** (gère Hibernate 7.4 nativement — cf. [matrix](https://hibernate.org/community/integrations/))
- Hibernate ORM **7.4.9.Final** (dernière 7.x stable)
- H2 **2.3.232**
- Kotlin **2.1.20**, `java.version=21`

## Références

- [Hibernate Migration Guide 7.0](https://docs.hibernate.org/orm/7.0/migration-guide/) — réécriture du plugin Maven
- [Hibernate Migration Guide 7.1](https://docs.hibernate.org/orm/7.1/migration-guide/) — options dépréciées, défauts re-`true`
- [User Guide — Bytecode Enhancement](https://docs.hibernate.org/orm/7.0/userguide/html_single/) — capabilities (lazy, dirty-tracking)
- [Hibernate / Spring Boot instabilité matrix](https://hibernate.org/community/integrations/)