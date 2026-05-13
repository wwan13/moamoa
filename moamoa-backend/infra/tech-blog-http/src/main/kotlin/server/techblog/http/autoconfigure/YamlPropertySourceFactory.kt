package server.techblog.http.autoconfigure

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.env.PropertiesPropertySource
import org.springframework.core.env.PropertySource
import org.springframework.core.io.support.EncodedResource
import org.springframework.core.io.support.PropertySourceFactory

internal class YamlPropertySourceFactory : PropertySourceFactory {

    override fun createPropertySource(name: String?, encodedResource: EncodedResource): PropertySource<*> {
        val properties = YamlPropertiesFactoryBean().apply {
            setResources(encodedResource.resource)
        }.getObject() ?: throw IllegalStateException("YAML property source를 읽을 수 없습니다.")

        val sourceName = name ?: encodedResource.resource.filename ?: "techBlogHttpProperties"
        return PropertiesPropertySource(sourceName, properties)
    }
}
