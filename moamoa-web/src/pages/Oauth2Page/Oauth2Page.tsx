import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner";
import {useEffect} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {showGlobalAlert} from "../../api/client";
import useAuth from "../../auth/useAuth";

const Oauth2Page = () => {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()

    const { socialLogin } = useAuth()

    useEffect(() => {
        const type = searchParams.get("type")

        if (type === "success") {
            const accessToken = searchParams.get("accessToken")
            const refreshToken = searchParams.get("refreshToken")
            const isNew = searchParams.get("isNew") === "true"

            socialLogin({accessToken, refreshToken, isNew})
            navigate(isNew ? "/?welcome=true" : "/")
        } else if (type === "hasError") {
            const errorMessage = searchParams.get("errorMessage")
            const errorAction = async () => {
                await showGlobalAlert(errorMessage)
                navigate("/")
            }
            errorAction()
        } else if (type === "emailRequired") {
            const provider = searchParams.get("provider")
            const providerKey = searchParams.get("providerKey")

            navigate(`/oauth2/email?from=${provider}&id=${providerKey}`)
        }

    }, [navigate, searchParams, socialLogin]);

    return (
        <>
            <GlobalSpinner />
        </>
    )
}

export default Oauth2Page
