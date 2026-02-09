import PageTitle from "../../components/pagetitle/PageTitle.tsx";
import {useEffect} from "react";
import {useNavigate} from "react-router-dom";

const MainPage = () => {

    const navigate = useNavigate()

    useEffect(() => {
        navigate("/dashboard")
    }, []);

    return <div>
        <PageTitle value={"모아모아 관리자"} />
    </div>
}

export default MainPage