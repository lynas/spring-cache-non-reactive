package com.lynas

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import java.util.concurrent.TimeUnit
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@EnableCaching
@SpringBootApplication
class DemoCacheNonReactiveApplication{
    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager("Customer")
            .apply {
                isAllowNullValues = false
                setCaffeine(
                    Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                )
            }
    }
}

fun main(args: Array<String>) {
    runApplication<DemoCacheNonReactiveApplication>(*args)
}


@Entity
data class Customer(
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    val id: Long?,
    val name: String
)

interface CustomerRepository : CrudRepository<Customer, Long>

@Service
@CacheConfig(cacheNames = ["Customer"])
class CustomerService(val customerRepository: CustomerRepository){
    @Cacheable
    fun findCustomerById(id: Long) = customerRepository.findById(id)
}



@RestController
class DemoController(val customerService: CustomerService){
    @GetMapping("/{id}")
    fun demoM(@PathVariable id: Long) = customerService.findCustomerById(id)
}