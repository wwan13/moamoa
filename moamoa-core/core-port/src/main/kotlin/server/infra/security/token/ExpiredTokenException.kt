package server.infra.security.token

class ExpiredTokenException : RuntimeException("TOKEN_EXPIRED")