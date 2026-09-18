# Hibernate 7+ Bytecode Enhancement — Exemples complets (Java & Kotlin)

Projets Maven fonctionnels qui prouvent que le bytecode enhancement de
[Hibernate ORM 7.x](https://hibernate.org/orm/releases/7.0/) fonctionne bien, avec le
nouveau plugin **`org.hibernate.orm:hibernate-maven-plugin`** (réécrit en 7.0).

Deux modules indépendants :

- [`java/`](./java) — entité POJO, champs privés, attribut lazy
- [`kotlin/`](./kotlin) — entité Kotlin (pièges `open` + `var`)

---

## Ce que ça valide (à l'exécution)

| Vérification | Java | Kotlin |
|---|---|---|
| `Product` implémente `ManagedEntity` + `SelfDirtinessTracker` (enhancement injecté) | ✅ | ✅ |
| Attribut `@Basic(fetch = LAZY)` chargé au premier accès | ✅ | ✅ |
| Re-merge **sans modification** → **0 UPDATE** (dirty-tracking enhancement) | ✅ | ✅ |
| Mutation réelle → **1 UPDATE** | ✅ | ✅ |

## Pré-requis

- **JDK 17+** (testé sous JDK 21)
- **Maven 3.8+**
- Base H2 embarquée (aucune config requise)

## Lancer

```bash
# Java
cd java
mvn package                       # compile + applique l'enhancement
mvn exec:java -Dexec.mainClass=com.example.hib.Main

# Kotlin
cd kotlin
mvn package
mvn exec:java -Dexec.mainClass=com.example.hib.MainKt
```

La sortie attendue (les deux langages) :

```
== ...Product.class enhanced (implements SelfDirtinessTracker) == true
== after accessing lazy description, length = NN
== unchanged merge: entity UPDATE statements = 0  (expect 0)
== changed name: entity UPDATE statements = 1  (expect 1)
ALL CHECKS PASSED
```

Pour voir le SQL instrumenté : `hibernate.show_sql=true` est déjà actif dans `Main`.

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
> ⚠️ Depuis **7.1.5**, `enableLazyInitialization` et `enableDirtyTracking` sont repassés
> à `true` par défaut (une régression 7.0 les avait mis à `false`). Sur 7.0.x,
> active-les explicitement.

### Options dépréciées en 7.1+

- `enableExtendedEnhancement` — utilise plutôt getters/setters + encapsulation
- `enableAssociationManagement` (bidirectionnel auto) — gère les deux côtés à la main
- l'enhancement runtime (`hibernate.enhancer.*`) — préférer l'enhancement au build

### Kotlin : piège à connaître

Les classes Kotlin sont **`final`** par défaut. L'enhancer ne peut pas tisser un
interceptor dans une classe finale :

```kotlin
@Entity
open class Product(          // <-- obligatoire : "open"
    var name: String,        // <-- "var", pas "val"
    // ...
)
```

---

## Versions

- Hibernate ORM **7.4.9.Final** (dernière 7.x stable)
- H2 **2.3.232**
- Kotlin **2.1.20**, `maven.compiler.release=17`

## Références

- [Hibernate Migration Guide 7.0](https://docs.hibernate.org/orm/7.0/migration-guide/) — réécriture du plugin Maven
- [Hibernate Migration Guide 7.1](https://docs.hibernate.org/orm/7.1/migration-guide/) — options dépréciées, défauts re-`true`
- [User Guide — Bytecode Enhancement](https://docs.hibernate.org/orm/7.0/userguide/html_single/) — capabilities (lazy, dirty-tracking)