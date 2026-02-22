import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const DEFAULT_P95_MS = Number(__ENV.P95_THRESHOLD_MS || 300);
const WARMUP_DURATION = '30s';
const MAIN_STAGES = [
    { duration: '1m', target: 50 },
    { duration: '2m', target: 50 },
    { duration: '30s', target: 0 },
];
const MAIN_DURATION_SECONDS = 60 + 120 + 30;

const PAGE_MIN = Number(__ENV.PAGE_MIN || 0);
const PAGE_MAX = Number(__ENV.PAGE_MAX || 1000);
const SIZE_MIN = Number(__ENV.SIZE_MIN || 1);
const SIZE_MAX = Number(__ENV.SIZE_MAX || 100);
const NULL_RATE = Number(__ENV.CONDITION_NULL_RATE || 0.15);
const QUERY_TERMS = (__ENV.QUERY_TERMS || 'kotlin,reactive,webflux,r2dbc,k6,load,test,post,spring,api')
    .split(',')
    .map((term) => term.trim())
    .filter((term) => term.length > 0);

const appRespTime = new Trend('app_resp_time', true);
const mainRequests = new Counter('main_requests');
const status200 = new Counter('status_200');
const status429 = new Counter('status_429');
const status5xx = new Counter('status_5xx');
const statusOther = new Counter('status_other');

const pageProvided = new Counter('page_provided');
const pageMissing = new Counter('page_missing');
const sizeProvided = new Counter('size_provided');
const sizeMissing = new Counter('size_missing');
const queryProvided = new Counter('query_provided');
const queryMissing = new Counter('query_missing');

export const options = {
    summaryTrendStats: ['min', 'med', 'avg', 'max', 'p(90)', 'p(95)', 'p(99)'],

    scenarios: {
        warmup: {
            executor: 'constant-vus',
            exec: 'warmup',
            vus: 5,
            duration: WARMUP_DURATION,
            gracefulStop: '0s',
        },
        main: {
            executor: 'ramping-vus',
            exec: 'main',
            startVUs: 0,
            stages: MAIN_STAGES,
            gracefulRampDown: '0s',
            gracefulStop: '0s',
            tags: { phase: 'main' },
        },
    },
    thresholds: {
        'http_req_failed{scenario:main}': ['rate<0.01'],
        [`http_req_duration{scenario:main}`]: [`p(95)<${DEFAULT_P95_MS}`],
        'http_reqs{scenario:main}': ['count>=0'],
    },
};

const baseUrl = (__ENV.BASE_URL || '').trim();
const authToken = (__ENV.AUTH_TOKEN || '').trim();
const endpoint = '/api/post';
const userAgent = 'moamoa-k6-get-post-random-conditions/1.0';

if (!baseUrl) {
    throw new Error('BASE_URL is required. Example: BASE_URL=https://example.com');
}

if (PAGE_MAX < PAGE_MIN) {
    throw new Error('PAGE_MAX must be greater than or equal to PAGE_MIN');
}

if (SIZE_MAX < SIZE_MIN) {
    throw new Error('SIZE_MAX must be greater than or equal to SIZE_MIN');
}

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

function randomTerm() {
    if (QUERY_TERMS.length === 0) {
        return `q-${randomInt(1, 1000000)}`;
    }

    const term = QUERY_TERMS[randomInt(0, QUERY_TERMS.length - 1)];
    return `${term}-${randomInt(1, 1000000)}`;
}

function maybeNull(factory) {
    if (Math.random() < NULL_RATE) {
        return null;
    }

    return factory();
}

function randomConditions() {
    return {
        page: maybeNull(() => randomInt(PAGE_MIN, PAGE_MAX)),
        size: maybeNull(() => randomInt(SIZE_MIN, SIZE_MAX)),
        query: maybeNull(() => randomTerm()),
    };
}

function buildQueryString(conditions) {
    const pairs = [];

    if (conditions.page !== null) {
        pairs.push(`page=${encodeURIComponent(String(conditions.page))}`);
        pageProvided.add(1);
    } else {
        pageMissing.add(1);
    }

    if (conditions.size !== null) {
        pairs.push(`size=${encodeURIComponent(String(conditions.size))}`);
        sizeProvided.add(1);
    } else {
        sizeMissing.add(1);
    }

    if (conditions.query !== null) {
        pairs.push(`query=${encodeURIComponent(conditions.query)}`);
        queryProvided.add(1);
    } else {
        queryMissing.add(1);
    }

    if (pairs.length === 0) {
        return '';
    }

    return `?${pairs.join('&')}`;
}

function buildHeaders() {
    const headers = {
        Accept: 'application/json',
        'User-Agent': userAgent,
    };

    if (authToken) {
        headers.Authorization = `Bearer ${authToken}`;
    }

    return headers;
}

function hitGetPost(isMain) {
    const conditions = randomConditions();
    const url = `${baseUrl}${endpoint}${buildQueryString(conditions)}`;

    const res = http.get(url, {
        headers: buildHeaders(),
        tags: { endpoint, method: 'GET' },
    });

    appRespTime.add(res.timings.duration, { scenario: isMain ? 'main' : 'warmup' });

    if (isMain) {
        mainRequests.add(1);
    }

    if (res.status === 200) {
        status200.add(1);
    } else if (res.status === 429) {
        status429.add(1);
    } else if (res.status >= 500 && res.status < 600) {
        status5xx.add(1);
    } else {
        statusOther.add(1);
    }

    const contentType = (res.headers['Content-Type'] || '').toLowerCase();
    const isJson = contentType.includes('application/json');

    let jsonParseOk = true;
    if (isJson) {
        try {
            JSON.parse(res.body);
        } catch (_e) {
            jsonParseOk = false;
        }
    }

    check(res, {
        'status is 200': (r) => r.status === 200,
        'json parse ok when content-type is json': () => jsonParseOk,
    });
}

export function warmup() {
    hitGetPost(false);
}

export function main() {
    hitGetPost(true);
}

function asNumber(metricValue) {
    if (typeof metricValue !== 'number' || Number.isNaN(metricValue)) {
        return 0;
    }
    return metricValue;
}

function pct(value) {
    return `${(value * 100).toFixed(2)}%`;
}

function fmtMs(value) {
    return `${value.toFixed(2)}ms`;
}

export function handleSummary(data) {
    const httpReqs = data.metrics.http_reqs?.values || {};
    const httpReqFailed = data.metrics.http_req_failed?.values || {};
    const httpReqDuration = data.metrics.http_req_duration?.values || {};
    const checks = data.metrics.checks?.values || {};

    const requestsCount = asNumber(httpReqs.count);
    const rps = asNumber(httpReqs.rate);

    const failRate = asNumber(httpReqFailed.rate);
    const failedRequests = Math.round(requestsCount * failRate);

    const avg = asNumber(httpReqDuration.avg);
    const p95 = asNumber(httpReqDuration['p(95)']);
    const p99 = asNumber(httpReqDuration['p(99)']);

    const status200Count = asNumber(data.metrics.status_200?.values?.count);
    const status429Count = asNumber(data.metrics.status_429?.values?.count);
    const status5xxCount = asNumber(data.metrics.status_5xx?.values?.count);
    const statusOtherCount = asNumber(data.metrics.status_other?.values?.count);
    const statusTotal = status200Count + status429Count + status5xxCount + statusOtherCount;

    const mainReqCount = asNumber(data.metrics.main_requests?.values?.count);
    const mainRps = mainReqCount / MAIN_DURATION_SECONDS;

    const checkPasses = asNumber(checks.passes);
    const checkFails = asNumber(checks.fails);
    const checkRate = asNumber(checks.rate);

    const pageProvidedCount = asNumber(data.metrics.page_provided?.values?.count);
    const pageMissingCount = asNumber(data.metrics.page_missing?.values?.count);
    const sizeProvidedCount = asNumber(data.metrics.size_provided?.values?.count);
    const sizeMissingCount = asNumber(data.metrics.size_missing?.values?.count);
    const queryProvidedCount = asNumber(data.metrics.query_provided?.values?.count);
    const queryMissingCount = asNumber(data.metrics.query_missing?.values?.count);

    const lines = [
        '=== GET /api/post Random Conditions Load Test Summary ===',
        `BASE_URL: ${baseUrl}`,
        `Total requests: ${requestsCount}`,
        `RPS (http_reqs.rate): ${rps.toFixed(2)}`,
        `TPS interpretation: TPS = RPS (use http_reqs.rate).`,
        `Main scenario RPS (main_requests/${MAIN_DURATION_SECONDS}s): ${mainRps.toFixed(2)}`,
        `Latency avg: ${fmtMs(avg)} | p95: ${fmtMs(p95)} | p99: ${fmtMs(p99)}`,
        `HTTP fail rate (http_req_failed.rate): ${pct(failRate)} (≈ ${failedRequests} reqs)`,
        `Checks: pass=${checkPasses}, fail=${checkFails}, rate=${pct(checkRate)}`,
        `Status distribution: 200=${status200Count}, 429=${status429Count}, 5xx=${status5xxCount}, other=${statusOtherCount}, total=${statusTotal}`,
        `Random condition distribution: page(provided=${pageProvidedCount}, missing=${pageMissingCount}), size(provided=${sizeProvidedCount}, missing=${sizeMissingCount}), query(provided=${queryProvidedCount}, missing=${queryMissingCount})`,
        '',
    ];

    const summaryJson = {
        test: 'GET /api/post (random conditions)',
        baseUrl,
        random_condition_config: {
            page_min: PAGE_MIN,
            page_max: PAGE_MAX,
            size_min: SIZE_MIN,
            size_max: SIZE_MAX,
            null_rate: NULL_RATE,
            query_terms_count: QUERY_TERMS.length,
        },
        thresholds: {
            http_req_failed_rate_lt: 0.01,
            http_req_duration_p95_lt_ms: DEFAULT_P95_MS,
        },
        throughput: {
            total_requests: requestsCount,
            rps_http_reqs_rate: Number(rps.toFixed(4)),
            tps_equals_rps_comment: 'TPS=RPS interpreted from http_reqs.rate',
            main_rps_derived: Number(mainRps.toFixed(4)),
        },
        latency_ms: {
            avg: Number(avg.toFixed(4)),
            p95: Number(p95.toFixed(4)),
            p99: Number(p99.toFixed(4)),
        },
        failure: {
            http_req_failed_rate: Number(failRate.toFixed(6)),
            http_req_failed_count_approx: failedRequests,
        },
        checks: {
            passes: checkPasses,
            fails: checkFails,
            rate: Number(checkRate.toFixed(6)),
        },
        status_distribution: {
            status_200: status200Count,
            status_429: status429Count,
            status_5xx: status5xxCount,
            status_other: statusOtherCount,
            total: statusTotal,
        },
        conditions_distribution: {
            page: {
                provided: pageProvidedCount,
                missing: pageMissingCount,
            },
            size: {
                provided: sizeProvidedCount,
                missing: sizeMissingCount,
            },
            query: {
                provided: queryProvidedCount,
                missing: queryMissingCount,
            },
        },
        raw_metrics: {
            http_reqs: httpReqs,
            http_req_failed: httpReqFailed,
            http_req_duration: httpReqDuration,
            checks,
            app_resp_time: data.metrics.app_resp_time?.values || {},
        },
    };

    return {
        stdout: `${lines.join('\n')}\n`,
        'summary.json': JSON.stringify(summaryJson, null, 2),
    };
}
