import {Route, Routes} from "react-router-dom"
import MainPage from "../pages/mainpage/MainPage"
import TechBlogsPage from "../pages/techblogspage/TechBlogsPage";
import TechBlogDetailPage from "../pages/techblogdetailpage/TechBlogDetailPage";
import MyPage from "../pages/mypage/MyPage";
import MySubscriptionPage from "../pages/mysubscriptionspage/MySubscriptionPage";
import DefaultLayout from "../layouts/DefaultLayout";
import BlankLayout from "../layouts/BlankLayout";
import SignupPage from "../pages/signuppage/SignupPage";
import Oauth2Page from "../pages/oauth2page/Oauth2Page";
import Oauth2EmailInputPage from "../pages/oauth2emailinputpage/Oauth2EmailInputPage";
import BlogSubmissionPage from "../pages/blogsubmissionpage/BlogSubmissionPage";
import PasswordChangePage from "../pages/passwordchangepage/PasswordChangePage";
import {MemberUnjoinPage} from "../pages/memberunjoinpage/MemberUnjoinPage";

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
