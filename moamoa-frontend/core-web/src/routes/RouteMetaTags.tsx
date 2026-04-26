import { useLocation } from "react-router-dom"
import { Helmet } from "react-helmet-async"
import { resolveRouteMeta } from "./routeMeta"

const RouteMetaTags = () => {
  const { pathname } = useLocation()
  const meta = resolveRouteMeta(pathname)

  return (
    <Helmet>
      <title>{meta.title}</title>
      <meta name="description" content={meta.description} />
      <meta name="robots" content={meta.robots} />

      <link rel="canonical" href={meta.canonicalUrl} />

      <meta property="og:type" content={meta.ogType} />
      <meta property="og:site_name" content={meta.siteName} />
      <meta property="og:title" content={meta.title} />
      <meta property="og:description" content={meta.description} />
      <meta property="og:url" content={meta.canonicalUrl} />
      <meta property="og:image" content={meta.ogImage} />

      <meta name="twitter:card" content="summary" />
      <meta name="twitter:title" content={meta.title} />
      <meta name="twitter:description" content={meta.description} />
      <meta name="twitter:image" content={meta.ogImage} />
    </Helmet>
  )
}

export default RouteMetaTags
