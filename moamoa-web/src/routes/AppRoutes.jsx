import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage.jsx"
import TechBlogsPage from "../pages/TechBlogsPage/TechBlogsPage.jsx";
import TechBlogDetailPage from "../pages/TechBlogDetailPage/TechBlogDetailPage.jsx";
import MyPage from "../pages/MyPage/MyPage.jsx";
import MySubscriptionPage from "../pages/MySubscriptionsPage/MySubscriptionPage.jsx";
import DefaultLayout from "../layouts/DefaultLayout.jsx";
import BlankLayout from "../layouts/BlankLayout.jsx";
import SignupPage from "../pages/SignupPage/SignupPage.jsx";
import Oauth2Page from "../pages/Oauth2Page/Oauth2Page.jsx";

export default function AppRoutes() {
    return (
        <Routes>
            <Route element={<DefaultLayout />}>
                <Route path="/" element={<MainPage />} />
                <Route path="/blogs" element={<TechBlogsPage />} />
                <Route path="/:key" element={<TechBlogDetailPage />} />
                <Route path="/subscription" element={<MySubscriptionPage />} />
                <Route path="/my" element={<MyPage />} />

                <Route path="/oauth2" element={<Oauth2Page />} />
            </Route>

            <Route element={<BlankLayout />}>
                <Route path="/signup" element={<SignupPage />} />
            </Route>
        </Routes>
    )
}