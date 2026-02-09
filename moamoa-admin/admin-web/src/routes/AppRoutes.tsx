import { Route, Routes } from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.tsx"
import LoginPage from "../pages/LoginPage/LoginPage.tsx"
import AppLayout from "../components/layout/AppLayout"
import DashboardPage from "../pages/DashboardPage/DashboardPage.tsx";
import PostsPage from "../pages/PostsPage/PostsPage.tsx";
import FeedbackPage from "../pages/FeedbackPage/FeedbackPage.tsx";
import SubmissionPage from "../pages/SubmissionPage/SubmissionPage.tsx";
import TechBlogPage from "../pages/TechBlogPage/TechBlogPage.tsx";

const AppRoutes = () => {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route path="/" element={<MainPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/post" element={<PostsPage />} />
                <Route path="/blog" element={<TechBlogPage />} />
                <Route path="/feedback" element={<FeedbackPage />} />
                <Route path="/submission" element={<SubmissionPage />} />
            </Route>
            <Route path="/login" element={<LoginPage />} />
        </Routes>
    )
}

export default AppRoutes
