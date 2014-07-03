package demobinding

import org.springframework.dao.DataIntegrityViolationException
import grails.converters.JSON

class DemoController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

    def index() {
        def items = Demo.list(params)
        def fields = ['code', 'name', 'name_es_ES', 'name_en_US', 'name_fr_FR', 'description', 'longDescription']

        def rows = items.collect { fields.collect { f -> it."$f" }.join(',') }
        render(text: ([fields.join(',')] + rows).join("\n<br>\n"))
    }

    def create() {
        [demoInstance: new Demo(params)]
    }

    def save() {
        def demoInstance = new Demo(params)
        if (!demoInstance.save(flush: true)) {
            render(view: "create", model: [demoInstance: demoInstance])
            return
        }

        flash.message = message(code: 'default.created.message', args: [message(code: 'demo.label', default: 'Demo'), demoInstance.id])
        redirect(action: "show", id: demoInstance.id)
    }

    def show(Long id) {
        def demoInstance = Demo.get(id)
        if (!demoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "list")
            return
        }

        [demoInstance: demoInstance]
    }

    def edit(Long id) {
        def demoInstance = Demo.get(id)
        if (!demoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "list")
            return
        }

        [demoInstance: demoInstance]
    }

    def update(Long id, Long version) {
        def demoInstance = Demo.get(id)
        if (!demoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "list")
            return
        }

        if (version != null) {
            if (demoInstance.version > version) {
                demoInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                          [message(code: 'demo.label', default: 'Demo')] as Object[],
                          "Another user has updated this Demo while you were editing")
                render(view: "edit", model: [demoInstance: demoInstance])
                return
            }
        }

        demoInstance.properties = params
        if (!demoInstance.save(flush: true)) {
            render(view: "edit", model: [demoInstance: demoInstance])
            return
        }

        flash.message = message(code: 'default.updated.message', args: [message(code: 'demo.label', default: 'Demo'), demoInstance.id])
        redirect(action: "show", id: demoInstance.id)
    }

    def delete(Long id) {
        def demoInstance = Demo.get(id)
        if (!demoInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "list")
            return
        }

        try {
            demoInstance.delete(flush: true)
            flash.message = message(code: 'default.deleted.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "list")
        }
        catch (DataIntegrityViolationException e) {
            flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'demo.label', default: 'Demo'), id])
            redirect(action: "show", id: id)
        }
    }
}
