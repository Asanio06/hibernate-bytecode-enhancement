package com.example.hib

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HibernateEnhancementApplication

fun main(args: Array<String>) {
    runApplication<HibernateEnhancementApplication>(*args)
}