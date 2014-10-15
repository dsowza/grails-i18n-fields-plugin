package i18nfields

import org.codehaus.groovy.grails.web.context.ServletContextHolder as SCH
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes as GA
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.exceptions.JedisConnectionException

/**
 * Keeps a Redis instance to be used with i18nFields.
 * @author fernando
 *
 */
class RedisHolder {
    private static final Logger log = LoggerFactory.getLogger(this)
    private static JedisPool pool = null

    public static def withJedis(Closure closure) {
        def jedis
        try {
            jedis = jedisPool.getResource()
            closure.call(jedis)

        } catch (JedisConnectionException ex) {
            if (jedis) {
                jedisPool.returnBrokenResource(jedis)
                jedis = null
            }
            log.warn "[i18n-fields] Jedis Connection Exception ${ ex }"

        } catch (Exception ex) {
            log.error "[i18n-fields] Error in withJedis ${ ex }"

        } finally {
            if (jedis) {
              jedisPool.returnResource(jedis)
            }
        }
    }

    /**
     * Get the jedisPool to be used.
     * @return
     */
    private static JedisPool getJedisPool() {
        if (!pool) {
            def config = getConfiguration()
            def poolConfig = new GenericObjectPoolConfig()

            poolConfig.setMaxTotal(config.pool?.maxTotal ?: 64)
            poolConfig.setMaxWaitMillis(config.pool?.maxWaitMillis ?: 500)

            poolConfig.setTestOnBorrow(true)
            poolConfig.setTestOnReturn(true)
            poolConfig.setTestWhileIdle(true)

            pool = new JedisPool(poolConfig, config.host, config.port,
                                 config.timeout ?: 500)

            log.info "[i18n-fields] Jedis pool created with config: $config"
        }
        pool
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
