import { Route, Routes } from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.tsx"
import LoginPage from "../pages/LoginPage/LoginPage.tsx"
import AppLayout from "../components/layout/AppLayout"
import DashboardPage from "../pages/DashboardPage/DashboardPage.tsx";
import UncategorizedPostsPage from "../pages/UncategorizedPostsPage/UncategorizedPostsPage.tsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<AppLayout />}>
                <Route path="/" element={<MainPage />} />
                <Route path="/dashboard" element={<DashboardPage />} />
                <Route path="/uncategorized" element={<UncategorizedPostsPage />} />
            </Route>
            <Route path="/login" element={<LoginPage />} />
        </Routes>
    )
}
