import PageTitle from "../../components/pagetitle/PageTitle.tsx";
import {useEffect} from "react";

const FeedbackPage = () => {
    useEffect(() => {
        window.scrollTo({ top: 0, left: 0, behavior: "auto" })
    }, [])

    return (
        <div>
            <PageTitle value="피드백" />
        </div>
    )
}

export default FeedbackPage
