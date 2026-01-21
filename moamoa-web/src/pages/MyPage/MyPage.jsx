import styles from "./MyPage.module.css"
import useAuth from "../../auth/AuthContext.jsx";
import {useEffect, useState} from "react";
import {useNavigate} from "react-router-dom";
import ArrowForwardIosIcon from '@mui/icons-material/ArrowForwardIos';
import GlobalSpinner from "../../components/GlobalSpinner/GlobalSpinner.jsx";
import BookmarkIcon from "@mui/icons-material/Bookmark";
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import {useMemberSummaryQuery} from "../../queries/member.queries.js";

const SKELETON_DELAY_MS = 0

export default function MyPage() {
    const { isLoggedIn, logout, isLogoutLoading } = useAuth()
    const navigate = useNavigate()

    const memberSummaryQuery = useMemberSummaryQuery()
    const member = memberSummaryQuery.data

    useEffect(() => {
        if (!isLoggedIn) {
            navigate("/")
        }
    }, [isLoggedIn]);

    const [showSkeleton, setShowSkeleton] = useState(false)
    useEffect(() => {
        let timer = null
        if (memberSummaryQuery.isPending) {
            timer = setTimeout(() => setShowSkeleton(true), SKELETON_DELAY_MS)
        } else {
            setShowSkeleton(false)
        }
        return () => timer && clearTimeout(timer)
    }, [memberSummaryQuery.isPending])

    return (
        <div className={styles.wrap}>
            { isLogoutLoading && <GlobalSpinner /> }
            { showSkeleton ? (
                <div>
                    <div className={`${styles.title} ${styles.skeleton} ${styles.skeletonTitle}`}></div>
                    <div className={styles.stats}>
                        <div className={styles.stat}>
                            <div className={styles.statTitle}>
                                <span className={styles.statLabel}>구독</span>
                                <LocalOfferIcon
                                    className={styles.statIcon}
                                    sx={{fontSize: 26, color: "#3B4953", fontWeight: 800}}
                                />
                            </div>
                            <div className={`${styles.statValue} ${styles.skeleton} ${styles.skeletonStat}`}></div>
                        </div>
                        <div className={styles.stat}>
                            <div className={styles.statTitle}>
                                <span className={styles.statLabel}>북마크</span>
                                <BookmarkIcon
                                    className={styles.statIcon}
                                    sx={{fontSize: 26, color: "#90AB8B", fontWeight: 800}}
                                />
                            </div>
                            <div className={`${styles.statValue} ${styles.skeleton} ${styles.skeletonStat}`}></div>
                        </div>
                    </div>
                </div>
            ) : (
                <div>
                    <p className={styles.title}>{member?.email}</p>
                    <div className={styles.stats}>
                        <div className={styles.stat}>
                            <div className={styles.statTitle}>
                                <span className={styles.statLabel}>구독</span>
                                <LocalOfferIcon
                                    className={styles.statIcon}
                                    sx={{fontSize: 26, color: "#3B4953", fontWeight: 800}}
                                />
                            </div>
                            <span className={styles.statValue}>{member?.subscribeCount}</span>
                        </div>
                        <div className={styles.stat}>
                            <div className={styles.statTitle}>
                                <span className={styles.statLabel}>북마크</span>
                                <BookmarkIcon
                                    className={styles.statIcon}
                                    sx={{fontSize: 26, color: "#90AB8B", fontWeight: 800}}
                                />
                            </div>
                            <span className={styles.statValue}>{member?.bookmarkCount}</span>
                        </div>
                    </div>
                </div>
            )}

            <section className={styles.section}>
                <p className={styles.sectionTitle}>활동</p>
                <div className={styles.buttons}>
                    <button className={styles.button} onClick={() => navigate("/subscription")}>
                        <p className={styles.buttonText}>모든 구독 블로그</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                    <div className={styles.divider}/>
                    <button className={styles.button} onClick={() => console.log("asd")}>
                        <p className={styles.buttonText}>블로그 요청 내역</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                </div>
            </section>

            <section className={styles.section}>
                <p className={styles.sectionTitle}>설정</p>
                <div className={styles.buttons}>
                    {/*<button className={styles.button}>*/}
                    {/*    <p className={styles.buttonText}>알림 설정</p>*/}
                    {/*    <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>*/}
                    {/*</button>*/}
                    {/*<div className={styles.divider}/>*/}

                    {member?.provider === "INTERNAL" && (
                        <>
                            <button className={styles.button} onClick={() => navigate("/password")}>
                                <p className={styles.buttonText}>비밀번호 변경</p>
                                <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                            </button>
                            <div className={styles.divider}/>
                        </>
                    )}
                    <button className={styles.button}>
                        <p className={styles.buttonText}>회원 탈퇴</p>
                        <ArrowForwardIosIcon sx={{ fontSize: 16, color: "#A8A8A8" }}/>
                    </button>
                </div>
            </section>

            <button
                className={styles.logoutButton}
                onClick={logout}
            >로그아웃</button>
        </div>
    )
}