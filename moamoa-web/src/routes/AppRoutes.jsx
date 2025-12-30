import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.jsx"
import TechBlogsPage from "../pages/TechBlogsPage/TechBlogsPage.jsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route path="/" element={<MainPage />} />
            <Route path="/blogs" element={<TechBlogsPage />} />
        </Routes>
    )
}