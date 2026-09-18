package com.example.hib

import jakarta.persistence.*

/**
 * Kotlin entity for Hibernate 7 bytecode enhancement.
 * IMPORTANT: the class must be `open` and fields `var` (Kotlin classes are `final`
 * by default, and the enhancer cannot weave into a final class).
 */
@Entity
@Table(name = "product_k")
open class Product(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    var name: String,

    @Basic(fetch = FetchType.LAZY)
    var description: String?,
)