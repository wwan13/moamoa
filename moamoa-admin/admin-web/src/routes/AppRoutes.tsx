import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.tsx";
import LoginPage from "../pages/LoginPage/LoginPage.tsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route>
                <Route path="/" element={<MainPage />} />
                <Route path="/login" element={<LoginPage />} />
            </Route>
        </Routes>
    )
}