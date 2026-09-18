package com.example.hib

import jakarta.persistence.*

/**
 * Kotlin entity with var properties (mutable).
 * NOTE: the class must be `open` — the bytecode enhancer cannot weave
 * trackers/interceptors into a `final` class, and Kotlin classes are final by default.
 */
@Entity
@Table(name = "product_k")
open class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    // lazy basic attribute
    @Basic(fetch = FetchType.LAZY)
    var description: String?,

    // Kotlin-specific: a non-null default property
    var category: String = "general",
)