package server.admin.feature.post.command.application

import org.springframework.stereotype.Service
import server.admin.feature.post.command.domain.AdminPostRepository
import server.admin.feature.posttag.domain.AdminPostTagRepository
import server.admin.feature.tag.domain.AdminTagRepository
import server.admin.feature.techblog.domain.AdminTechBlogRepository
import server.admin.infra.transaction.AdminTransactional
import server.techblog.TechBlogSources

@Service
internal class AdminPostService
