import org.codehaus.groovy.grails.commons.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import i18nfields.*
import grails.util.GrailsUtil
import org.codehaus.groovy.grails.orm.hibernate.HibernateEventListeners
import org.hibernate.event.PostInsertEvent
import org.codehaus.groovy.grails.commons.GrailsClassUtils

class I18nFieldsGrailsPlugin {
    static final Logger log = LoggerFactory.getLogger(this)

    def groupId = "com.ticketbis"
    def version = "1.1.0"
    def grailsVersion = "2.1.0 > *"
    def pluginExcludes = [
            "lib/*",
            "grails-app/i18n/*",
            "grails-app/domain/i18nfields/*",
            "grails-app/controllers/i18nfields/*",
            "grails-app/services/i18nfields/*",
            "grails-app/taglib/i18nfields/*",
            "grails-app/views/*",
            "grails-app/views/layouts/*",
            "web-app/css/*",
            "web-app/images/*",
            "web-app/images/skin/*",
            "web-app/js/*",
            "web-app/js/prototype/*",
    ]
    def dependsOn = [:]

    def config = ConfigurationHolder.config
    def locales = config."${ I18nFields.I18N_FIELDS }"?."${ I18nFields.LOCALES }"

    def author = "Jorge Uriarte"
    def authorEmail = "jorge.uriarte@omelas.net"
    def title = "i18n Fields"
    def description = "This plugin provides an easy way of declarativily localize database fields of your content tables."
    def documentation = "http://grails.org/plugin/i18n-fields"

    def grailsApplication
    def setApplication(app) {
        log.info "Grails Application injected"
        grailsApplication = app
    }

    def addLocalizedProperties(clzz, field, locales) {
        def proxyGetter = GrailsClassUtils.getGetterName(field)
        clzz.metaClass."$proxyGetter" = {
            i18nfields.I18nFieldsHelper.getValueOrDefault(delegate, field)
        }

        def getterWithLocale = GrailsClassUtils.getGetterName("${ field }OrEmpty")
        clzz.metaClass."$getterWithLocale" = { locale ->
            i18nfields.I18nFieldsHelper.getValueOrEmpty(delegate, field, locale)
        }

        locales.each { locale ->
            log.info "Adding virtual getters and setters for $clzz ($field, $locale)"
            def getter = GrailsClassUtils.getGetterName("${field}_${locale}")
            def setter = GrailsClassUtils.getSetterName("${field}_${locale}")
            clzz.metaClass."$getter" = {
                i18nfields.I18nFieldsHelper.getValueOrDefault(delegate, field, locale)
            }
            clzz.metaClass."$setter" = { value ->
                i18nfields.I18nFieldsHelper.setValue(delegate, field, locale, value)
            }
        }
    }

    def doWithDynamicMethods = { context ->
        log.info "Plugin version: $version"
        ['domain', 'controller', 'service', 'bootstrap'].each {
            log.info "Adding 'withLocale' method to '${it}' classes"
            application."${it}Classes".each { theClass ->
                theClass.metaClass.withLocale = I18nFieldsHelper.withLocale
            }
        }
        ['domain'].each {
            log.info "Adding saveLocale method to ${it} classes"
            application."${it}Classes".each { theClass ->
                theClass.metaClass.saveLocale = I18nFieldsHelper.saveLocale
            }
        }

        application.domainClasses.each { domainClass ->
            //if (!domainClass.getClazz().hasProperty(I18nFields.I18N_FIELDS))
            //    return

            def fields
            try {
                fields = domainClass.getClazz()."${ I18nFields.I18N_FIELDS }"
            } catch (Exception ex) {
                return
            }

            if (fields && locales) {
                fields.each { field ->
                    addLocalizedProperties(domainClass, field, locales)
                }
            }
        }

        i18nfields.I18nFieldsHelper.metaClass.getApplicationContext = { -> context }
    }

    def doWithSpring = {
    }

    def doWithApplicationContext = { applicationContext ->
        def listeners = applicationContext.sessionFactory.eventListeners
        def listener = new I18nFieldsListener()
        ['saveOrUpdate', 'preDelete'].each { type ->
            def typeProperty = "${type}EventListeners"
            def typeListeners = listeners."${typeProperty}"

            def expandedTypeListeners = new Object[typeListeners.length + 1]
            System.arraycopy(typeListeners, 0, expandedTypeListeners, 0, typeListeners.length)
            expandedTypeListeners[-1] = listener

            listeners."${typeProperty}" = expandedTypeListeners
        }
    }
}

