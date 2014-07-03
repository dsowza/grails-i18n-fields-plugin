class BootStrap {

    def init = { servletContext ->
        // Agregamos un dato demo
        def spanish = new demobinding.Demo(code: 'es')
        spanish.name_es_ES = "Español"
        spanish.name_fr_FR = "Espagnol"
        spanish.name_en_US = "Spanish"
        spanish.description_es_ES = "Idioma español"
        spanish.description_fr_FR = "Lange espagnol"
        spanish.description_en_US = "Spanish language"
        spanish.longDescription_es_ES = "Una descripción de Español..."
        spanish.save(failOnError: true)

        def french = new demobinding.Demo(code: 'fr')
        french.name_es_ES = "Francés"
        french.name_fr_FR = "Français"
        french.name_en_US = "French"
        french.description_es_ES = "Idioma francés"
        french.description_fr_FR = "Lange français"
        french.description_en_US = "French language"
        french.longDescription_es_ES = "Une description en français..."
        french.save(failOnError: true)

        def english = new demobinding.Demo(code: 'en')
        english.name_es_ES = "Inglés"
        english.name_fr_FR = "Anglais"
        english.name_en_US = "English"
        english.description_es_ES = "Idioma inglés"
        english.description_fr_FR = "Lange anglais"
        english.description_en_US = "English language"
        english.longDescription_es_ES = "A description in english..."
        english.save(failOnError: true)

    }

    def destroy = {
    }
}
