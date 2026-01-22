import styles from "./MemberUnjoinPage.module.css"
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import {useState} from "react";
import Button from "../../components/ui/Button.jsx";
import {useMemberSummaryQuery, useUnjoinMutation} from "../../queries/member.queries.js";
import {showGlobalAlert, showGlobalConfirm, showToast} from "../../api/client.js";
import {useNavigate} from "react-router-dom";
import useAuth from "../../auth/AuthContext.jsx";

export function MemberUnjoinPage() {
    const [agreementChecked, setAgreementChecked] = useState(false)
    const [totalError, setTotalError] = useState("")
    const navigate = useNavigate()

    const {logoutProcess} = useAuth()

    const memberSummaryQuery = useMemberSummaryQuery()
    const member = memberSummaryQuery.data

    const unjoinMutate = useUnjoinMutation()

    const validateChecked = () => {
        if (!agreementChecked) {
            setTotalError("필수 동의 항목에 동의해 주세요.")
        }
    }

    const onSubmit = async () => {
        validateChecked()

        try {
            await showGlobalConfirm({
                message: "탈퇴하시겠습니까?"
            })
            await unjoinMutate.mutateAsync()
            logoutProcess()
            showToast("탈퇴되었습니다.")
            navigate("/")
        } catch (e) {
            showGlobalAlert("다시 시도해 주세요")
        }
    }

    return (
        <div className={styles.wrap}>
            {unjoinMutate.isPending && (
                <GlobalSpinner />
            )}
            <div className={styles.titleWrap}>
                <span className={styles.title}>회원 탈퇴</span>
                <span className={styles.description}>
                    <div className={styles.descriptionTitle}>탈퇴하기 전에</div>
                    · 탈퇴시 모아모아에 등록한 <b>모든 정보가 영구적으로 삭제됩니다.</b> <br/>
                    · 삭제된 정보는 <b>복구가 불가합니다.</b>
                </span>
            </div>

            <div className={styles.divider}/>

            <div className={styles.accountInfo}>
                <p className={styles.accountTitle}>탈퇴하려는 계정</p>
                <div className={styles.account}>
                    {memberSummaryQuery.isPending ? (
                        <div className={styles.accountSkeleton}></div>
                    ) : (
                        <>{member?.email}</>
                    )}
                </div>
            </div>

            <div className={styles.divider}/>

            <div className={styles.agreement}>
                <div className={styles.checkWrap}>
                    <input
                        type="checkbox"
                        checked={agreementChecked}
                        onChange={(e) => {
                            setAgreementChecked(e.target.checked)
                            if (totalError) setTotalError("")
                        }}
                        onBlur={validateChecked}
                    />
                    <span>회원 탈퇴를 진행하여 계정에 귀속된 모든 정보를 삭제하는 데 동의합니다. (필수)</span>
                </div>
                {
                    totalError !== "" && (
                        <div className={styles.totalError}>
                            <span className={styles.error}>✕ {totalError}</span>
                        </div>
                    )
                }
            </div>

            <Button onClick={onSubmit} type="submit">회원 탈퇴</Button>
        </div>
    )
}