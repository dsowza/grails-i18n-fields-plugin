package demobinding

@i18nfields.I18nFields
class Demo {
    String code
    String name
    String description
    String longDescription

    static constraints = {
        code unique: true
        name blank: false
    }

    static i18nFields = ['name', 'description', 'longDescription']
    //static i18nFieldsRename = ['name': 'nombre']
}
