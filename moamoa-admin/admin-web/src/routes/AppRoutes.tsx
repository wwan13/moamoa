import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.tsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route>
                <Route path="/" element={<MainPage />} />
            </Route>
        </Routes>
    )
}