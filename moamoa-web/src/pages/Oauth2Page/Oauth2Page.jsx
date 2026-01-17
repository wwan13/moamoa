import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import {useEffect, useState} from "react";
import {useNavigate, useSearchParams} from "react-router-dom";
import {showGlobalAlert} from "../../api/client.js";
import useAuth from "../../auth/AuthContext.jsx";

export default function Oauth2Page() {
    const [isLoading, setIsLoading] = useState(true)
    const [searchParams, setSearchParams] = useSearchParams()
    const navigate = useNavigate()

    const { socialLogin } = useAuth()

    useEffect(() => {
        setIsLoading(true)
        const type = searchParams.get("type")

        if (type === "success") {
            const accessToken = searchParams.get("accessToken")
            const refreshToken = searchParams.get("refreshToken")
            const isNew = searchParams.get("isNes")

            socialLogin({accessToken, refreshToken, isNew})
            navigate(`/${isNew ?? "?welcome=true"}`)
        } else if (type === "hasError") {
            const errorMessage = searchParams.get("errorMessage")
            const errorAction = async () => {
                setIsLoading(false)
                await showGlobalAlert(errorMessage)
                navigate("/")
            }
            errorAction()
        } else if (type === "emailRequired") {
            const provider = searchParams.get("provider")
            const providerKey = searchParams.get("providerKey")

            navigate(`/oauth2/email?from=${provider}&id=${providerKey}`)
        }

    }, []);

    return (
        <>
            { isLoading && <GlobalSpinner />}
        </>
    )
}