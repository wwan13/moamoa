rootProject.name = "moamoa"

include("moamoa-core")
include("moamoa-core:core-api")
include("moamoa-core:core-batch")
include("moamoa-core:core-tech-blog")
include("moamoa-core:core-shared")

include("moamoa-infra")
include("moamoa-infra:infra-redis")
include("moamoa-infra:infra-tech-blog")
include("moamoa-infra:infra-lmstudio")
include("moamoa-infra:infra-mailgun")
include("moamoa-infra:infra-jwt")
include("moamoa-infra:infra-crypto")
include("moamoa-infra:infra-caffeine")
include("moamoa-infra:infra-cache")
include("moamoa-infra:infra-lock")

include("moamoa-support")
include("moamoa-support:support-api-docs")
include("moamoa-support:support-templates")
include("moamoa-support:support-monitoring")
include("moamoa-support:support-webhook")
include("moamoa-support:support-logging")
include("moamoa-support:support-test")

include("moamoa-admin")
include("moamoa-admin:admin-api")
include("moamoa-admin:admin-web")

include("moamoa-web")
