import styles from "./MemberUnjoinPage.module.css"
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import {useEffect, useState} from 'react';
import Button from "../../components/ui/Button.jsx";
import {useMemberSummaryQuery, useUnjoinMutation} from "../../queries/member.queries.js";
import {showGlobalAlert, showGlobalConfirm, showToast} from "../../api/client.js";
import {useNavigate} from "react-router-dom";
import useAuth from "../../auth/AuthContext.jsx";

export function MemberUnjoinPage() {
    const [agreementChecked, setAgreementChecked] = useState(false)
    const navigate = useNavigate()

    const {logoutProcess, isLoggedIn} = useAuth()

    const memberSummaryQuery = useMemberSummaryQuery()
    const member = memberSummaryQuery.data

    const unjoinMutate = useUnjoinMutation()

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }
    }, [isLoggedIn]);

    const onSubmit = async () => {
        try {
            await unjoinMutate.mutateAsync()
            logoutProcess()
            showToast("탈퇴가 완료되었습니다.")
            navigate("/")
        } catch (e) {
            showGlobalAlert("다시 시도해 주세요.")
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
                    <div className={styles.descriptionTitle}>탈퇴 시 유의사항</div>
                    • 탈퇴 시 모아모아에 등록한 모든 정보가 영구적으로 삭제됩니다. <br/>
                    • 삭제된 정보는 복구가 불가합니다.
                    <br/><br/>
                    <div className={styles.descriptionTitle}>탈퇴 후 재가입 규정</div>
                    • 탈퇴 회원이 재가입하더라도 기존의 정보는 이미 소멸되었기 때문에 복구되지 않습니다.
                </span>
            </div>

            <div className={styles.divider}/>

            <div className={styles.accountInfo}>
                <p className={styles.accountTitle}>회원탈퇴 계정 정보</p>
                <div className={styles.account}>
                    {memberSummaryQuery.isPending ? (
                        <div className={styles.accountSkeleton}></div>
                    ) : (
                        <>
                            <span className={styles.accountBold}>{member?.email} </span>
                            {member?.provider === "GOOGLE" && "(구글 계정 연동)"}
                            {member?.provider === "GITHUB" && "(깃허브 계정 연동)"}
                        </>
                    )}
                </div>
            </div>

            <div className={styles.agreement}>
                <div className={styles.checkWrap}>
                    <input
                        type="checkbox"
                        checked={agreementChecked}
                        onChange={(e) => {
                            setAgreementChecked(e.target.checked)
                            if (totalError) setTotalError("")
                        }}
                    />
                    <span>위 내용을 모두 확인했습니다.</span>
                </div>
            </div>

            <div className={styles.buttonWrap}>
                <Button
                    variant={"border"}
                    fullWidth={false}
                    onClick={() => window.history.go(-1)}
                    type="submit"
                >취소</Button>
                <Button
                    variant={"primary"}
                    fullWidth={false}
                    onClick={onSubmit}
                    type="submit"
                    disabled={!agreementChecked}
                >회원 탈퇴</Button>
            </div>
        </div>
    )
}