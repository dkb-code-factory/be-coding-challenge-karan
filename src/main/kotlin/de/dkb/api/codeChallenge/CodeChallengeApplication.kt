package de.dkb.api.codeChallenge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableCaching
class CodeChallengeApplication

fun main(args: Array<String>) {
	runApplication<CodeChallengeApplication>(*args)
}
