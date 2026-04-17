import { useEffect, useRef } from "react"
import { useNavigate } from "react-router-dom"
import useAuth from "../../auth/useAuth"

const LoginRedirectPage = () => {
  const navigate = useNavigate()
  const { openLogin } = useAuth()
  const redirectedRef = useRef(false)

  useEffect(() => {
    if (redirectedRef.current) return
    redirectedRef.current = true

    openLogin()
    navigate("/", { replace: true })
  }, [navigate, openLogin])

  return null
}

export default LoginRedirectPage
