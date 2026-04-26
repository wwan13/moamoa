import styles from "./NotFoundPage.module.css"

const NotFoundPage = () => {
  return (
    <section className={styles.wrap}>
      <svg
        className={styles.illustration}
        viewBox="0 0 680 520"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-labelledby="notFoundTitle notFoundDescription"
      >
        <title id="notFoundTitle">페이지를 찾을 수 없습니다</title>
        <desc id="notFoundDescription">
          요청하신 페이지를 찾을 수 없다는 안내 일러스트
        </desc>

        <ellipse
          cx="340"
          cy="420"
          rx="70"
          ry="12"
          fill="#0E4BBC"
          opacity="0.15"
        />

        <g>
          <rect
            x="230"
            y="170"
            width="220"
            height="165"
            rx="22"
            fill="#0E4BBC"
          />
          <rect
            x="245"
            y="183"
            width="190"
            height="135"
            rx="14"
            fill="#0A3A99"
          />
          <rect
            x="253"
            y="191"
            width="174"
            height="119"
            rx="10"
            fill="#082D80"
          />

          <text
            x="340"
            y="244"
            textAnchor="middle"
            fontFamily="'Arial Black', sans-serif"
            fontWeight="900"
            fontSize="42"
            fill="#0E4BBC"
            opacity="0.4"
          >
            404
          </text>
          <text
            x="340"
            y="244"
            textAnchor="middle"
            fontFamily="'Arial Black', sans-serif"
            fontWeight="900"
            fontSize="42"
            fill="white"
            opacity="0.9"
          >
            404
          </text>

          <rect x="380" y="256" width="3" height="16" rx="1" fill="#4D80E0">
            <animate
              attributeName="opacity"
              values="0.8;0;0.8"
              dur="1s"
              repeatCount="indefinite"
            />
          </rect>

          <g>
            <ellipse cx="303" cy="310" rx="20" ry="22" fill="white" />
            <ellipse cx="377" cy="310" rx="20" ry="22" fill="white" />
            <ellipse cx="306" cy="313" rx="11" ry="13" fill="#0A3A99" />
            <ellipse cx="374" cy="313" rx="11" ry="13" fill="#0A3A99" />
            <ellipse
              cx="310"
              cy="307"
              rx="4"
              ry="5"
              fill="white"
              opacity="0.7"
            />
            <ellipse
              cx="378"
              cy="307"
              rx="4"
              ry="5"
              fill="white"
              opacity="0.7"
            />
            <circle cx="301" cy="318" r="2" fill="white" opacity="0.4" />
            <circle cx="369" cy="318" r="2" fill="white" opacity="0.4" />
          </g>

          <path
            d="M315 345 Q340 333 365 345"
            fill="none"
            stroke="white"
            strokeWidth="3.5"
            strokeLinecap="round"
            opacity="0.9"
          />

          <ellipse
            cx="287"
            cy="338"
            rx="13"
            ry="8"
            fill="#FF6B9D"
            opacity="0.35"
          />
          <ellipse
            cx="393"
            cy="338"
            rx="13"
            ry="8"
            fill="#FF6B9D"
            opacity="0.35"
          />

          <rect x="325" y="335" width="30" height="32" rx="5" fill="#0A3A99" />
          <rect x="290" y="363" width="100" height="16" rx="8" fill="#0A3A99" />

          <path
            d="M232 250 Q195 270 185 300"
            fill="none"
            stroke="#0E4BBC"
            strokeWidth="20"
            strokeLinecap="round"
          />
          <circle cx="185" cy="302" r="12" fill="#0E4BBC" />
          <path
            d="M448 250 Q485 270 495 300"
            fill="none"
            stroke="#0E4BBC"
            strokeWidth="20"
            strokeLinecap="round"
          />
          <circle cx="495" cy="302" r="12" fill="#0E4BBC" />

          <g>
            <path
              d="M305 378 Q295 400 288 420"
              fill="none"
              stroke="#0E4BBC"
              strokeWidth="18"
              strokeLinecap="round"
            />
            <ellipse cx="286" cy="423" rx="18" ry="11" fill="#0A3A99" />
          </g>
          <g>
            <path
              d="M375 378 Q385 400 392 420"
              fill="none"
              stroke="#0E4BBC"
              strokeWidth="18"
              strokeLinecap="round"
            />
            <ellipse cx="394" cy="423" rx="18" ry="11" fill="#0A3A99" />
          </g>
        </g>

        <text
          x="430"
          y="165"
          fontSize="36"
          fontWeight="900"
          fontFamily="Arial"
          fill="#0E4BBC"
          opacity="0.5"
        >
          ?
        </text>
        <text
          x="228"
          y="158"
          fontSize="24"
          fontWeight="900"
          fontFamily="Arial"
          fill="#0E4BBC"
          opacity="0.3"
        >
          ?
          <animate
            attributeName="opacity"
            values="0.3;0.6;0.3"
            dur="2.5s"
            begin="0.8s"
            repeatCount="indefinite"
          />
        </text>
        <text
          x="462"
          y="210"
          fontSize="18"
          fontWeight="900"
          fontFamily="Arial"
          fill="#0E4BBC"
          opacity="0.25"
        >
          ?
          <animate
            attributeName="opacity"
            values="0.25;0.55;0.25"
            dur="3s"
            begin="1.5s"
            repeatCount="indefinite"
          />
        </text>

        <circle cx="190" cy="185" r="4" fill="#4D80E0" opacity="0.5">
          <animate
            attributeName="opacity"
            values="0.5;1;0.5"
            dur="2s"
            repeatCount="indefinite"
          />
        </circle>
        <circle cx="500" cy="165" r="3" fill="#4D80E0" opacity="0.4">
          <animate
            attributeName="opacity"
            values="0.4;0.9;0.4"
            dur="1.8s"
            begin="0.5s"
            repeatCount="indefinite"
          />
        </circle>
        <circle cx="175" cy="360" r="3" fill="#4D80E0" opacity="0.35">
          <animate
            attributeName="opacity"
            values="0.35;0.7;0.35"
            dur="2.3s"
            begin="1s"
            repeatCount="indefinite"
          />
        </circle>
        <circle cx="510" cy="370" r="5" fill="#4D80E0" opacity="0.3">
          <animate
            attributeName="opacity"
            values="0.3;0.65;0.3"
            dur="2.8s"
            begin="0.3s"
            repeatCount="indefinite"
          />
        </circle>

        <text
          x="340"
          y="465"
          textAnchor="middle"
          fontFamily="'Pretendard', 'Noto Sans KR', sans-serif"
          fontSize="22"
          fontWeight="700"
          fill="#0E4BBC"
          opacity="0.85"
        >
          앗, 길을 잃었어요!
        </text>
        <text
          x="340"
          y="492"
          textAnchor="middle"
          fontFamily="'Pretendard', 'Noto Sans KR', sans-serif"
          fontSize="14"
          fontWeight="400"
          fill="#0E4BBC"
          opacity="0.5"
        >
          요청하신 페이지를 찾을 수 없습니다
        </text>
      </svg>
    </section>
  )
}

export default NotFoundPage
