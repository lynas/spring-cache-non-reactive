package com.lynas

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import java.util.*
import java.util.concurrent.TimeUnit
import javax.persistence.Entity
import javax.persistence.Id


@EnableCaching
@SpringBootApplication
class DemoCacheNonReactiveApplication

@Configuration
class AppConfig {

    @Bean
    fun cacheManager(): CacheManager {
        return CaffeineCacheManager("Student")
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
data class Student(

    @Id
    val id: Long?,
    val name: String
)

interface StudentRepository : CrudRepository<Student, Long>

@Service
@CacheConfig(cacheNames = ["Student"])
class StudentService(val studentRepository: StudentRepository) {

    @Cacheable(key = "#id")
    fun findStudentById(id: Long) = studentRepository.findById(id)

    @CacheEvict(key = "#id")
    fun updateStudent(id: Long, student: Student) {
        val existingStudent = this.findStudentById(id).get().copy(name = student.name)
        studentRepository.save(existingStudent)
    }
}


@RestController
class StudentController(val studentService: StudentService, val cacheManager: CacheManager) {

    @GetMapping("/{id}")
    fun getStudent(@PathVariable id: Long) = studentService.findStudentById(id)

    @GetMapping("/update/{id}")
    fun updateStudent(@PathVariable id: Long) =
        studentService.updateStudent(id, Student(id, "Name ${UUID.randomUUID()}"))


    @GetMapping("/all")
    fun allCache(): MutableCollection<String> = cacheManager.cacheNames
}