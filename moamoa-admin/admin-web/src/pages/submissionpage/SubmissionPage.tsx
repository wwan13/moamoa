import PageTitle from "../../components/pagetitle/PageTitle.tsx";
import {useEffect} from "react";

const SubmissionPage = () => {
    useEffect(() => {
        window.scrollTo({ top: 0, left: 0, behavior: "auto" })
    }, [])

    return (
        <div>
            <PageTitle value="블로그 요청" />
        </div>
    )
}

export default SubmissionPage
