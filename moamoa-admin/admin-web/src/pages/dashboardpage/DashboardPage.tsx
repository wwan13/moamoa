import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { useEffect } from "react"

const DashboardPage = () => {
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  return (
    <div>
      <PageTitle value="모아모아 관리자" />
    </div>
  )
}

export default DashboardPage
