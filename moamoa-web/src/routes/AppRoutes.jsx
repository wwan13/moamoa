import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.jsx"

export default function AppRoutes() {
    return (
        <Routes>
            <Route path="/" element={<MainPage />} />
        </Routes>
    )
}