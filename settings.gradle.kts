rootProject.name = "moamoa"

include("moamoa-core")
include("moamoa-core:core-api")
include("moamoa-core:core-batch")
include("moamoa-core:core-port")

include("moamoa-client")
include("moamoa-client:client-tech-blogs")
include("moamoa-client:client-mail")

include("moamoa-infra")
include("moamoa-infra:infra-redis")

include("moamoa-support")
include("moamoa-support:support-api-docs")
include("moamoa-support:support-templates")

include("moamoa-admin")

include("moamoa-infra:infra-security")