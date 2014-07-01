grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"

// Uncomment and edit the following lines to start using Grails encoding & escaping improvements

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    appenders {
        console name: 'stdout', layout: pattern(conversionPattern: '%d{yyyy-MM-dd/HH:mm:ss.SSS} [%t] %x %-5p %c{2} - %m%n')
    }

    root {
        warn()
    }

    environments {
        test {
            info 'stdout': ['grails.app', 'i18nfields','I18nFieldsGrailsPlugin']
        }
    }
}

/* remove this line
// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'none' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}
remove this line */
