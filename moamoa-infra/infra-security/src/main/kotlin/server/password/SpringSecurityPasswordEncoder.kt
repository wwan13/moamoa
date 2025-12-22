package server.password

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import server.infra.security.password.PasswordEncoder

@Component
class SpringSecurityPasswordEncoder : PasswordEncoder {

    private val bCryptPasswordEncoder = BCryptPasswordEncoder()

    override fun encode(rawPassword: String): String {
        return bCryptPasswordEncoder.encode(rawPassword)
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return bCryptPasswordEncoder.matches(rawPassword, encodedPassword)
    }
}