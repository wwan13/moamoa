import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.jsx"
import TechBlogsPage from "../pages/TechBlogsPage/TechBlogsPage.jsx";
import TechBlogDetailPage from "../pages/TechBlogDetailPage/TechBlogDetailPage.jsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route path="/" element={<MainPage />} />
            <Route path="/blogs" element={<TechBlogsPage />} />
            <Route path="/:key" element={<TechBlogDetailPage />} />
        </Routes>
    )
}