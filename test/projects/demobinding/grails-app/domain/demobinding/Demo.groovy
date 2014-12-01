package demobinding

//import i18nfields.Translatable

//@Mixin(Translatable)
class Demo {
    public i18nFieldInfo = [:]

    String code
    String name_es_ES
    String description_es_ES
    String longDescription_es_ES

    static constraints = {
        code unique: true
        name_es_ES blank: false
    }

    static i18nFields = ['name', 'description', 'longDescription']
    //static i18nFieldsRename = ['name': 'nombre']
}
