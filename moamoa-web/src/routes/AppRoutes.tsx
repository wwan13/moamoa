import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/MainPage/MainPage"
import TechBlogsPage from "../pages/TechBlogsPage/TechBlogsPage";
import TechBlogDetailPage from "../pages/TechBlogDetailPage/TechBlogDetailPage";
import MyPage from "../pages/MyPage/MyPage";
import MySubscriptionPage from "../pages/MySubscriptionsPage/MySubscriptionPage";
import DefaultLayout from "../layouts/DefaultLayout";
import BlankLayout from "../layouts/BlankLayout";
import SignupPage from "../pages/SignupPage/SignupPage";
import Oauth2Page from "../pages/Oauth2Page/Oauth2Page";
import Oauth2EmailInputPage from "../pages/Oauth2EmailInputPage/Oauth2EmailInputPage";
import BlogSubmissionPage from "../pages/BlogSubmissionPage/BlogSubmissionPage";
import PasswordChangePage from "../pages/PasswordChangePage/PasswordChangePage";
import {MemberUnjoinPage} from "../pages/MemberUnjoinPage/MemberUnjoinPage";

const AppRoutes = () => {
    return (
        <Routes>
            <Route element={<DefaultLayout />}>
                <Route path="/" element={<MainPage />} />
                <Route path="/blogs" element={<TechBlogsPage />} />
                <Route path="/:techBlogId" element={<TechBlogDetailPage />} />
                <Route path="/subscription" element={<MySubscriptionPage />} />
                <Route path="/my" element={<MyPage />} />
                <Route path="/submission" element={<BlogSubmissionPage />} />
                <Route path="/password" element={<PasswordChangePage />} />
                <Route path="/unjoin" element={<MemberUnjoinPage />} />

                <Route path="/oauth2" element={<Oauth2Page />} />
            </Route>

            <Route element={<BlankLayout />}>
                <Route path="/signup" element={<SignupPage />} />
                <Route path="/oauth2/email" element={<Oauth2EmailInputPage />} />
            </Route>
        </Routes>
    )
}
export default AppRoutes
