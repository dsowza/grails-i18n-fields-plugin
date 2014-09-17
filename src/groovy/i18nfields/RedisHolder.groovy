package i18nfields

import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

/**
 * Keeps a Redis instance to be used with i18nFields.
 * @author fernando
 *
 */
class RedisHolder {
    private static final Logger log = LoggerFactory.getLogger(this)
    private static JedisPool pool = null

    /**
     * Get the redisInstance to be used.
     * @return
     */
    public static Jedis getInstance() {
        if (!pool) {
            def config = getConfiguration()
            def poolConfig = new GenericObjectPoolConfig()

            poolConfig.setMaxTotal(config?.pool?.maxTotal ?: 64)

            // if we have a timeout, use timeout constuctor
            if (configuration.timeout) {
                pool = new JedisPool(poolConfig, config.host, config.port, config.timeout)
            } else {
                pool = new JedisPool(poolConfig, config.host, config.port)
            }

            log.info "Jedis pool created with config: $config"
        }
        return pool.getResource()
    }

    private static def getSpringBean(String name) {
        SCH.getServletContext().getAttribute(GA.APPLICATION_CONTEXT).getBean(name)
    }

    private static def getConfiguration() {
        def config = [host: 'localhost', port: 6379]
        def applicationConfig =
          getSpringBean("grailsApplication").config[I18nFields.I18N_FIELDS].redisConfig

        if (applicationConfig)
          config << applicationConfig

        return config
    }

    private RedisHolder() { }
}
