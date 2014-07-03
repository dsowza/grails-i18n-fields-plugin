package i18nfields

import org.codehaus.groovy.grails.compiler.injection.GrailsASTUtils

import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.expr.ListExpression
import org.codehaus.groovy.ast.expr.MapExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement

import org.codehaus.groovy.ast.FieldNode
import static org.springframework.asm.Opcodes.*
import java.lang.reflect.Modifier
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.builder.AstBuilder
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.ast.PropertyNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 * Responsable of transforming a Domain class with i18n fields and helpers.
 */
class ClassI18nalizator {
    private static final Logger log = LoggerFactory.getLogger(this)

    private final ClassNode classNode
    private final Collection<Locale> locales
    private final Collection<Locale> redisLocales
    private final Boolean constraintsEnforce

    private final AstBuilder astBuilder = new AstBuilder()

    /**
     * Initialize a ClassI18nalizator
     *
     * @param classNode Class being transformed
     * @param locales List of all knock locales
     * @param redisLocales List of locales stored in redis
     * @param constraintsEnforce Copy constraints from field to redis locales
     */
    ClassI18nalizator(ClassNode classNode, Collection<Locale> locales, Collection<Locale> redisLocales, Boolean constraintsEnforce) {
        this.classNode = classNode
        this.locales = locales
        this.redisLocales = redisLocales
        this.constraintsEnforce = constraintsEnforce
    }

    /**
     * Execute the transformation
     */
    void transformClass() {
        addHelper()
        addInformationHolder()
        addFieldsAndAccessors()
    }

    /**
     * Adds the i18nFieldsHelpers to the class.
     * This helper is used to retrieve and store the values. It is added and initialized.
     */
    private void addHelper() {
        def helperField =
            new FieldNode("i18nFieldsHelper",
                          ACC_PUBLIC | ACC_FINAL | ACC_STATIC,
                          new ClassNode(I18nFieldsHelper.class),
                          classNode,
                          new ConstructorCallExpression(new ClassNode(I18nFieldsHelper.class),
                                                        MethodCallExpression.NO_ARGUMENTS))
        classNode.addField(helperField)
    }

    /**
     * Adds a map to hold information for the object
     */
    private void addInformationHolder() {
        def informationHolderField =
            new FieldNode(I18nFields.DATA,
                          ACC_PUBLIC,
                          new ClassNode(Object.class),
                          classNode,
                          new MapExpression())

        classNode.addField(informationHolderField)
    }

    /**
     * Add the fields and methods to support locales for every i18n field.
     */
    private void addFieldsAndAccessors() {
        i18nFieldList.each { fieldName ->
            addI18nFields(fieldName)

            // Remove the original field and associated constraints.
            removeField(fieldName)
            removeConstraintsFor(fieldName)

            addGettersAndSetters(fieldName)
        }
    }

    /**
     * Add a static field of a given type to the ClassNode
     */
    private addStaticField(name, initialExpression) {
        def field = new FieldNode(name, ACC_PUBLIC | ACC_STATIC, new ClassNode(Object.class), classNode, initialExpression)
        field.setDeclaringClass(classNode)
        classNode.addField(field)
    }

    private addField(name, initialExpression) {
        def field = new FieldNode(name, ACC_PUBLIC, new ClassNode(Object.class), classNode, initialExpression)

        log.debug "Adding ${name} field to ${classNode.name}"
        field.setDeclaringClass(classNode)
        classNode.addField(field)

    }

    private getI18nFieldList() {
        if (!fieldExists(I18nFields.I18N_FIELDS)) {
            log.warn "Visted ${classNode.name} but no ${I18nFields.I18N_FIELDS} static property found"
            return []
        }
        def i18nFieldsListField = classNode.getField(I18nFields.I18N_FIELDS)
        def configuredI18nFields = i18nFieldsListField.getInitialValueExpression()?.expressions*.getText()

        def i18nFields = configuredI18nFields.findAll { fieldExists(it) }
        def invalidI18nFields = configuredI18nFields - i18nFields
        if (invalidI18nFields)
            log.warn "Ignoring ${invalidI18nFields} in ${classNode.name} non existant field(s)"

        return i18nFields
    }

    /**
     * Remove a property from the classNode
     */
    private removeField(String name) {
        classNode.properties.remove(classNode.getProperty(name))
        classNode.removeField(name)
    }

    /**
     * Make field Transient by adding the field to the Transients list
     * @param name name of the field to make transient.
     */
    private makeFieldTransient(String name) {
        def transients = getOrCreateTransientsField().getInitialExpression()
        transients.addExpression(new ConstantExpression(name))
    }

    /**
     * Add to a field the Bindable constraint.
     * @param name name of the field to make bindable.
     */
    private makeFieldBindable(String name) {
        def astNodes = new AstBuilder().buildFromString("${name}(bindable: true)");
        def blockStatement = astNodes.get(0);
        def listStatements = blockStatement.getStatements();
        def returnStatement = listStatements.get(0);
        def expression = returnStatement.getExpression();

        def constraints = getOrCreateConstraintsField().getInitialExpression();
        def blockStatement2 = constraints.getCode();
        blockStatement2.addStatement(new ExpressionStatement(expression));
    }

    /**
     * Gets or create a static field for properties like Transients or Constraints.
     * @params field to get or create.
     * @params type type of the field to create.
     */
    private getOrCreateField(name, type) {
        if (!fieldExists(name)) addStaticField(name, type)
        return classNode.getDeclaredField(name)
    }

    /**
     * Get or create the Transients property
     */
    private getOrCreateTransientsField() {
        return getOrCreateField(I18nFields.TRANSIENTS, new ListExpression());
    }

    /**
     * Get or create the Constraints property
     * Constraints field is a closure without parameters and initialy a empty block
     */
    private getOrCreateConstraintsField() {
        def closureExpression = new ClosureExpression(Parameter.EMPTY_ARRAY, new BlockStatement());
        closureExpression.setVariableScope(new VariableScope())

        return  getOrCreateField(I18nFields.CONSTRAINTS, closureExpression);
    }

    /**
     * Add fields for each locale needed
     * @param baseName base fieldname to be created in each locale.
     */
    private addI18nFields(baseName) {
        locales.each { locale ->
            def fieldName = "${baseName}_${locale}"

            // Create localized field and copy constraints.
            addI18nField(baseName, locale.toString())
            if (!hasConstraints(fieldName) && hasConstraints(baseName)) {
                if(!isRedisLocale(locale) || constraintsEnforce)
                    copyConstraints(baseName, fieldName)
            }

            // If it is a redisLocale, then make the field transient and bindable
            if (isRedisLocale(locale)) {
                makeFieldTransient(fieldName)
                makeFieldBindable(fieldName)
            }
        }
    }

    /**
     * Check if a locale will be stored on redis
     * @param locale locale to check
     * @return true if redis locale, false otherwise.
     */
    private isRedisLocale(locale) {
        return redisLocales.contains(locale)
    }

    /**
     * Adds a String Field to the class.
     */
    private addI18nField(String name, String locale) {
        classNode.addProperty("${name}_${locale}", Modifier.PUBLIC, new ClassNode(String.class), new ConstantExpression(null), null, null)
        addPropertyGetter(name, locale)
        addPropertySetter(name, locale)
    }

    private def buildSetterMethod(field) {
        // setter should return void. that's why the return statement.
        astBuilder.buildFromString("""
          i18nfields.I18nFieldsHelper.setValue(this, '${field}', value)
          return
        """).pop()
    }

    private boolean hasConstraints(field) {
        return hasConstraints() && null != getConstraints(field)
    }

    private boolean hasConstraints() {
        return fieldExists(I18nFields.CONSTRAINTS)
    }

    /**
     * True if the field exists, otherwise false
     */
    private boolean fieldExists(name) {
        return null != classNode.getDeclaredField(name)
    }

    /**
     * Get the constraint block for a field.
     */
    private getConstraints(field) {
        def closure = getConstraints().getInitialExpression().getCode()
        return closure.statements.find { statement ->
            containsAMethodCallExpression(statement) && field == statement.getExpression().getMethodAsString()
        }
    }

    /**
     * Get the constraints closure.
     */
    private getConstraints() {
        return classNode.getDeclaredField(I18nFields.CONSTRAINTS)
    }

    /**
     * Remove all the constraints for a field
     */
    private removeConstraintsFor(field) {
        def block = getConstraints()?.getInitialExpression()
        if (block) {
            def filtered = block.getCode().statements.findAll {
                it.getExpression().getMethodAsString() != field
            }
            block.setCode(new BlockStatement(filtered, block.variableScope))
        }
    }

    /**
     * Check if a statement is a method call.
     */
    private boolean containsAMethodCallExpression(statement) {
        statement instanceof ExpressionStatement && statement.getExpression() instanceof MethodCallExpression
    }

    /**
     * Copy constraints for one field to another.
     */
    private copyConstraints(from, to) {
        def baseMethodCall = getConstraints(from).getExpression()
        def methodCall = new MethodCallExpression(new VariableExpression('this'), to, baseMethodCall.getArguments())
        def newConstraints = new ExpressionStatement(methodCall)
        addConstraints(newConstraints)
    }

    /**
     * Add new constraints to new fields.
     */
    private addConstraints(constraints) {
        def closure = getConstraints().getInitialExpression().getCode()
        closure.addStatement(constraints)
    }

    private addPropertyGetter(field, locale) {
        String methodName = GrailsClassUtils.getGetterName("${field}_${locale}")
        def code = new AstBuilder().buildFromString("""
            i18nfields.I18nFieldsHelper.getValueOrDefault(this, "$field", "$locale")
        """).pop()

        def methodNode = new MethodNode(
            methodName,
            ACC_PUBLIC,
            ClassHelper.STRING_TYPE,
            Parameter.EMPTY_ARRAY,
            ClassHelper.EMPTY_TYPE_ARRAY,
            code
        );
        classNode.addMethod(methodNode)
    }

    private addPropertySetter(field, locale) {
        String methodName = GrailsClassUtils.getSetterName("${field}_${locale}");
        def params = new Parameter[1]
        def methodNode = new MethodNode(
            methodName,
            ACC_PUBLIC,
            ClassHelper.VOID_TYPE,
            [new Parameter(ClassHelper.STRING_TYPE, "value")] as Parameter[],
            ClassHelper.EMPTY_TYPE_ARRAY,
            buildSetterMethod(methodName)
        );
        classNode.addMethod(methodNode)
    }

    /**
     * Add a Delegate Gatter and Setter for a field.
     */
    private addGettersAndSetters(field) {
        addProxyGetter(field)
        addLocalizedGetter(field)
        addLocalizedGetterEmpty(field)
    }

    /**
     *
     */
    private addProxyGetter(field) {
        String methodName = GrailsClassUtils.getGetterName(field);
        def code = new AstBuilder().buildFromString("""
            i18nfields.I18nFieldsHelper.getValueOrDefault(this, "$field")
        """).pop();

        def methodNode = new MethodNode(
            methodName,
            ACC_PUBLIC,
            ClassHelper.STRING_TYPE,
            Parameter.EMPTY_ARRAY,
            ClassHelper.EMPTY_TYPE_ARRAY,
            code
        );

        classNode.addMethod(methodNode);
    }

    private addLocalizedGetter(field) {
        def methodName = GrailsClassUtils.getGetterName(field)
        def code = new AstBuilder().buildFromString("""
            i18nfields.I18nFieldsHelper.getValueOrDefault(this, "$field", locale)
        """).pop();

        def parameters = [new Parameter(ClassHelper.make(Locale, false), "locale")] as Parameter[]

        def methodNode = new MethodNode(
            methodName,
            ACC_PUBLIC,
            ClassHelper.STRING_TYPE,
            parameters,
            ClassHelper.EMPTY_TYPE_ARRAY,
            code
        );

        classNode.addMethod(methodNode)
    }

    private addLocalizedGetterEmpty(field) {
        def methodName = GrailsClassUtils.getGetterName(field) + "OrEmpty"
        def code = new AstBuilder().buildFromString("""
            i18nfields.I18nFieldsHelper.getValueOrEmpty(this, "$field", locale)
        """).pop();

        def parameters = [new Parameter(ClassHelper.make(Locale, false), "locale")] as Parameter[]

        def methodNode = new MethodNode(
            methodName,
            ACC_PUBLIC,
            ClassHelper.STRING_TYPE,
            parameters,
            ClassHelper.EMPTY_TYPE_ARRAY,
            code
        );

        classNode.addMethod(methodNode)
    }
}
