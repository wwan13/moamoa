import styles from "./MaintenancePage.module.css"

const MaintenancePage = () => {
  return (
    <section className={styles.wrap}>
      <svg
        className={styles.illustration}
        viewBox="0 0 680 540"
        xmlns="http://www.w3.org/2000/svg"
        role="img"
        aria-labelledby="maintenanceTitle maintenanceDescription"
      >
        <title id="maintenanceTitle">서비스 점검 중</title>
        <desc id="maintenanceDescription">
          더 나은 서비스를 위해 점검을 진행 중이라는 안내 일러스트
        </desc>

        <ellipse
          cx="340"
          cy="428"
          rx="70"
          ry="11"
          fill="#0E4BBC"
          opacity="0.13"
        />

        <g>
          <ellipse cx="340" cy="186" rx="122" ry="12" fill="#FFD166" />
          <path
            d="M218 186 Q218 140 340 130 Q462 140 462 186 Z"
            fill="#FFD166"
          />
          <path
            d="M225 178 Q340 168 455 178"
            fill="none"
            stroke="#F4B800"
            strokeWidth="4"
            strokeLinecap="round"
            opacity="0.6"
          />

          <rect
            x="230"
            y="188"
            width="220"
            height="152"
            rx="22"
            fill="#0E4BBC"
          />
          <rect
            x="245"
            y="200"
            width="190"
            height="122"
            rx="14"
            fill="#0A3A99"
          />
          <rect
            x="253"
            y="208"
            width="174"
            height="106"
            rx="10"
            fill="#082D80"
          />

          <text
            x="340"
            y="238"
            textAnchor="middle"
            fontFamily="'Arial Black', sans-serif"
            fontWeight="900"
            fontSize="22"
            fill="white"
            opacity="0.85"
          >
            점검 중
          </text>
          <rect
            x="275"
            y="248"
            width="130"
            height="12"
            rx="6"
            fill="#0E4BBC"
            opacity="0.5"
          />
          <rect x="275" y="248" height="12" rx="6" fill="#FFD166" opacity="0.9">
            <animate
              attributeName="width"
              values="20;110;80;120;20"
              dur="3s"
              repeatCount="indefinite"
            />
          </rect>
          <text
            x="340"
            y="278"
            textAnchor="middle"
            fontFamily="Arial"
            fontSize="11"
            fill="white"
            opacity="0.5"
          >
            잠시만 기다려 주세요...
          </text>

          <g>
            <ellipse cx="297" cy="316" rx="18" ry="17" fill="white" />
            <ellipse cx="383" cy="316" rx="18" ry="17" fill="white" />
            <ellipse cx="299" cy="316" rx="10" ry="11" fill="#0A3A99" />
            <ellipse cx="385" cy="316" rx="10" ry="11" fill="#0A3A99" />
            <ellipse
              cx="303"
              cy="311"
              rx="4"
              ry="4"
              fill="white"
              opacity="0.7"
            />
            <ellipse
              cx="389"
              cy="311"
              rx="4"
              ry="4"
              fill="white"
              opacity="0.7"
            />
          </g>

          <path
            d="M282 300 Q297 294 312 298"
            fill="none"
            stroke="white"
            strokeWidth="3.5"
            strokeLinecap="round"
            opacity="0.9"
          />
          <path
            d="M368 298 Q383 294 398 300"
            fill="none"
            stroke="white"
            strokeWidth="3.5"
            strokeLinecap="round"
            opacity="0.9"
          />
          <path
            d="M316 344 Q340 352 364 344"
            fill="none"
            stroke="white"
            strokeWidth="3.5"
            strokeLinecap="round"
            opacity="0.85"
          />

          <ellipse
            cx="280"
            cy="334"
            rx="13"
            ry="8"
            fill="#FF9BD2"
            opacity="0.28"
          />
          <ellipse
            cx="400"
            cy="334"
            rx="13"
            ry="8"
            fill="#FF9BD2"
            opacity="0.28"
          />

          <rect x="325" y="340" width="30" height="30" rx="5" fill="#0A3A99" />
          <rect x="290" y="367" width="100" height="15" rx="7" fill="#0A3A99" />

          <path
            d="M232 258 Q195 260 182 280"
            fill="none"
            stroke="#0E4BBC"
            strokeWidth="20"
            strokeLinecap="round"
          />
          <circle cx="180" cy="282" r="12" fill="#0E4BBC" />
          <path
            d="M448 258 Q488 270 498 300"
            fill="none"
            stroke="#0E4BBC"
            strokeWidth="20"
            strokeLinecap="round"
          />
          <circle cx="498" cy="302" r="12" fill="#0E4BBC" />

          <g>
            <path
              d="M305 380 Q295 402 287 422"
              fill="none"
              stroke="#0E4BBC"
              strokeWidth="18"
              strokeLinecap="round"
            />
            <ellipse cx="285" cy="425" rx="18" ry="11" fill="#0A3A99" />
          </g>
          <g>
            <path
              d="M375 380 Q385 402 393 422"
              fill="none"
              stroke="#0E4BBC"
              strokeWidth="18"
              strokeLinecap="round"
            />
            <ellipse cx="395" cy="425" rx="18" ry="11" fill="#0A3A99" />
          </g>
        </g>

        <g>
          <path
            d="M168 268 Q162 255 170 245 Q178 235 188 240 L182 250 L176 258 Z"
            fill="#aaa"
            opacity="0.9"
          />
          <rect
            x="170"
            y="255"
            width="10"
            height="36"
            rx="5"
            fill="#bbb"
            opacity="0.9"
            transform="rotate(30 175 273)"
          />
          <path
            d="M182 286 Q186 296 180 304 Q174 312 165 308 L170 298 L176 292 Z"
            fill="#aaa"
            opacity="0.9"
          />
        </g>

        <g opacity="0.4">
          <circle
            cx="510"
            cy="195"
            r="16"
            fill="none"
            stroke="#FFD166"
            strokeWidth="3"
          >
            <animateTransform
              attributeName="transform"
              type="rotate"
              from="0 510 195"
              to="360 510 195"
              dur="6s"
              repeatCount="indefinite"
            />
          </circle>
          <circle
            cx="510"
            cy="195"
            r="8"
            fill="none"
            stroke="#FFD166"
            strokeWidth="2"
          >
            <animateTransform
              attributeName="transform"
              type="rotate"
              from="360 510 195"
              to="0 510 195"
              dur="6s"
              repeatCount="indefinite"
            />
          </circle>
        </g>
        <g opacity="0.25">
          <circle
            cx="175"
            cy="175"
            r="12"
            fill="none"
            stroke="#FFD166"
            strokeWidth="2.5"
          >
            <animateTransform
              attributeName="transform"
              type="rotate"
              from="0 175 175"
              to="360 175 175"
              dur="4s"
              repeatCount="indefinite"
            />
          </circle>
        </g>

        <text
          x="340"
          y="472"
          textAnchor="middle"
          fontFamily="'Pretendard','Noto Sans KR',sans-serif"
          fontSize="22"
          fontWeight="700"
          fill="#0E4BBC"
          opacity="0.85"
        >
          서비스 점검 중이에요
        </text>
        <text
          x="340"
          y="498"
          textAnchor="middle"
          fontFamily="'Pretendard','Noto Sans KR',sans-serif"
          fontSize="14"
          fill="#0E4BBC"
          opacity="0.5"
        >
          더 나은 서비스를 위해 준비 중입니다
        </text>
      </svg>
    </section>
  )
}

export default MaintenancePage
