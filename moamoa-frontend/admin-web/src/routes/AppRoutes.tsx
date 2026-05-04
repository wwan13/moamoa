import { Navigate, Route, Routes } from "react-router-dom"
import MainPage from "../pages/mainpage/MainPage.tsx"
import LoginPage from "../pages/loginpage/LoginPage.tsx"
import AppLayout from "../components/layout/AppLayout"
import DashboardPage from "../pages/dashboardpage/DashboardPage.tsx"
import NoticeCreatePage from "../pages/noticecreatepage/NoticeCreatePage.tsx"
import NoticeListPage from "../pages/noticelistpage/NoticeListPage.tsx"
import PostsPage from "../pages/postspage/PostsPage.tsx"
import FeedbackPage from "../pages/feedbackpage/FeedbackPage.tsx"
import SubmissionPage from "../pages/submissionpage/SubmissionPage.tsx"
import TechBlogPage from "../pages/techblogpage/TechBlogPage.tsx"
import NotFoundPage from "../pages/notfoundpage/NotFoundPage.tsx"
import CachePage from "../pages/cachepage/CachePage.tsx"

const AppRoutes = () => {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route path="/" element={<MainPage />} />
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/notice" element={<NoticeListPage />} />
        <Route path="/notice/create" element={<NoticeCreatePage />} />
        <Route path="/post" element={<PostsPage />} />
        <Route path="/blog" element={<TechBlogPage />} />
        <Route path="/cache" element={<CachePage />} />
        <Route path="/feedback" element={<FeedbackPage />} />
        <Route path="/submission" element={<SubmissionPage />} />
      </Route>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/404" element={<NotFoundPage />} />
      <Route path="*" element={<Navigate to="/404" replace />} />
    </Routes>
  )
}

export default AppRoutes
