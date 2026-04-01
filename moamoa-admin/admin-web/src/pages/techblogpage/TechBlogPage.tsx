import PageTitle from "../../components/pagetitle/PageTitle.tsx"
import { useEffect } from "react"

const TechBlogPage = () => {
  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })
  }, [])

  return (
    <div>
      <PageTitle value="기술블로그" />
    </div>
  )
}

export default TechBlogPage
