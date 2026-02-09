import { useContext } from "react"
import { AuthContext } from "./context"
import type { AuthContextValue } from "./context"

const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext)
  if (!context) throw new Error("useAuth must be used within AuthProvider")
  return context
}

export default useAuth
