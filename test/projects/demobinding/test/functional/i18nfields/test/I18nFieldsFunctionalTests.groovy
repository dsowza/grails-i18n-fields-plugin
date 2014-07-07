package i18nfields.test

import functionaltestplugin.FunctionalTestCase

class I18nFieldsFunctionalTests extends FunctionalTestCase {

    void testCorrectTranslations() {
        [es_ES: 'Español', fr_FR: 'Espagnol'].each { lang, text ->
            get "/demo/showCode/es?lang=$lang"
            assertStatus 200
            assertContent text
        }
    }

    void testCorrectTranslationsForDBField() {
        [es: 'Español', fr: 'Francés'].each { lang, text ->
            get "/demo/showCodeByLang/$lang?lang=en_US&fieldLang=es_ES"
            assertStatus 200
            assertContent text
        }
    }

    void testCorrectTranslationsForRedisField() {
        [es: 'Spanish', fr: 'French'].each { lang, text ->
            get "/demo/showCodeByLang/$lang?lang=es_ES&fieldLang=en_US"
            assertStatus 200
            assertContent text
        }
    }

    void testFallbackTranslations() {
        [es_PE: 'Español', en_GB: 'Spanish'].each { lang, text ->
            get "/demo/showCode/es?lang=$lang"
            assertStatus 200
            assertContent text
        }
    }



}
