package i18nfields

import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import redis.clients.jedis.Jedis

/**
 * Keeps a Redis instance to be used with i18nFields.
 * @author fernando
 *
 */
class RedisHolder {
    private static final Logger log = LoggerFactory.getLogger(this)
    private static Jedis redisInstance = null

    /**
     * Get the redisInstance to be used.
     * @return
     */
    public static Jedis getInstance() {
        /* TODO! This fixes weird bug that blocks app when RedisOutputStream buffer is full */
        redisInstance = null
        if (!redisInstance) {
            def config = getConfiguration()

            // if we have a timeout, use timeout constuctor
            if (configuration.timeout) {
                redisInstance = new Jedis(config.host, config.port, config.timeout)
            } else {
                redisInstance = new Jedis(config.host, config.port)
            }

            log.info "Jedis connected with config: $config"
        }
        return redisInstance
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
