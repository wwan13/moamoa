import { useEffect, useRef } from "react"
import { useNavigate, useParams } from "react-router-dom"
import { showGlobalAlert } from "../../api/client"
import { useViewPostMutation } from "../../queries/post.queries"

const PostRedirectPage = () => {
  const { postId } = useParams()
  const navigate = useNavigate()
  const { mutate } = useViewPostMutation()
  const requestedRef = useRef(false)

  useEffect(() => {
    window.scrollTo({ top: 0, left: 0, behavior: "auto" })

    if (requestedRef.current) return
    requestedRef.current = true

    const resolvedPostId = Number(postId)
    if (!Number.isFinite(resolvedPostId) || resolvedPostId <= 0) {
      navigate("/", { replace: true })
      return
    }

    mutate(
      { postId: resolvedPostId },
      {
        onSuccess: (post) => {
          const targetUrl = post?.url?.trim()
          if (targetUrl) {
            window.location.replace(targetUrl)
            return
          }

          navigate("/", { replace: true })
        },
        onError: async () => {
          await showGlobalAlert({
            title: "오류",
            message: "게시글을 불러오는데 오류가 발생했습니다.",
          })
          navigate("/", { replace: true })
        },
      },
    )
  }, [mutate, navigate, postId])

  return null
}

export default PostRedirectPage
