import { Navigate, Route, Routes } from "react-router-dom"
import MainPage from "../pages/mainpage/MainPage"
import TechBlogsPage from "../pages/techblogspage/TechBlogsPage"
import TechBlogDetailPage from "../pages/techblogdetailpage/TechBlogDetailPage"
import MyPage from "../pages/mypage/MyPage"
import MySubscriptionPage from "../pages/mysubscriptionspage/MySubscriptionPage"
import DefaultLayout from "../layouts/DefaultLayout"
import BlankLayout from "../layouts/BlankLayout"
import SignupPage from "../pages/signuppage/SignupPage"
import Oauth2Page from "../pages/oauth2page/Oauth2Page"
import Oauth2EmailInputPage from "../pages/oauth2emailinputpage/Oauth2EmailInputPage"
import PostRedirectPage from "../pages/postredirectpage/PostRedirectPage"
import BlogSubmissionPage from "../pages/blogsubmissionpage/BlogSubmissionPage"
import PasswordChangePage from "../pages/passwordchangepage/PasswordChangePage"
import { MemberUnjoinPage } from "../pages/memberunjoinpage/MemberUnjoinPage"
import PrivacyPolicyPage from "../pages/privacypolicy/PrivacyPolicyPage.tsx"
import NoticeListPage from "../pages/noticelistpage/NoticeListPage.tsx"
import FindPasswordPage from "../pages/findpasswordpage/FindPasswordPage.tsx"
import NoticeDetailPage from "../pages/noticedetailpage/NoticeDetailPage.tsx"
import MaintenancePage from "../pages/maintenancepage/MaintenancePage.tsx"
import NotFoundPage from "../pages/notfoundpage/NotFoundPage.tsx"
import LoginRedirectPage from "../pages/loginredirectpage/LoginRedirectPage.tsx"

const AppRoutes = () => {
  return (
    <Routes>
      <Route element={<DefaultLayout />}>
        <Route path="/" element={<MainPage />} />
        <Route path="/blogs" element={<TechBlogsPage />} />
        <Route path="/blog/:techBlogId" element={<TechBlogDetailPage />} />
        <Route path="/subscription" element={<MySubscriptionPage />} />
        <Route path="/my" element={<MyPage />} />
        <Route path="/submission" element={<BlogSubmissionPage />} />
        <Route path="/password" element={<PasswordChangePage />} />
        <Route path="/unjoin" element={<MemberUnjoinPage />} />
        <Route path="/notice" element={<NoticeListPage />} />
        <Route path="/notice/:noticeId" element={<NoticeDetailPage />} />

        <Route path="/privacy" element={<PrivacyPolicyPage />} />
        <Route path="/login" element={<LoginRedirectPage />} />

        <Route path="/oauth2" element={<Oauth2Page />} />

        <Route path="/maintenance" element={<MaintenancePage />} />
        <Route path="/404" element={<NotFoundPage />} />
      </Route>

      <Route element={<BlankLayout />}>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/find/password" element={<FindPasswordPage />} />
        <Route path="/oauth2/email" element={<Oauth2EmailInputPage />} />
        <Route path="/post/:postId" element={<PostRedirectPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  )
}
export default AppRoutes
