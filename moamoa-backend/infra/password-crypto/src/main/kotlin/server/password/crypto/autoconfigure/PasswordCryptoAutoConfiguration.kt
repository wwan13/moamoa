package server.password.crypto.autoconfigure

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Import
import server.password.CryptoPasswordEncoder

@AutoConfiguration
@ConditionalOnMissingBean(name = ["cryptoPasswordEncoder"])
@Import(CryptoPasswordEncoder::class)
class PasswordCryptoAutoConfiguration
