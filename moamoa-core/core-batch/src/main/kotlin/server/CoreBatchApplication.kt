package server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CoreBatchApplication

fun main(args: Array<String>) {
    runApplication<CoreBatchApplication>(*args)
}