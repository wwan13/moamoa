rootProject.name = "moamoa"

include("moamoa-core")
include("moamoa-core:core-api")
include("moamoa-core:core-batch")
include("moamoa-core:core-tech-blog")

include("moamoa-infra")
include("moamoa-infra:infra-redis")
include("moamoa-infra:infra-mail")
include("moamoa-infra:infra-tech-blog")
include("moamoa-infra:infra-security")
include("moamoa-infra:infra-ai")

include("moamoa-support")
include("moamoa-support:support-api-docs")
include("moamoa-support:support-templates")
include("moamoa-support:support-monitoring")
include("moamoa-support:support-webhook")
include("moamoa-support:support-logging")

include("moamoa-admin")
include("moamoa-admin:admin-api")

include("moamoa-web")