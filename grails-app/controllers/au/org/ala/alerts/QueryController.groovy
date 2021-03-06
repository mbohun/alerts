package au.org.ala.alerts

import au.org.ala.alerts.Query
import org.springframework.dao.DataIntegrityViolationException

class QueryController {

    static allowedMethods = [save: "POST", update: "POST", delete: "POST"]
    def authService

    def index() {
        redirect(action: "list", params: params)
    }

    def list() {
      if(authService.userInRole("ROLE_ADMIN") ){
        params.max = Math.min(params.max ? params.int('max') : 300, 1000)
        [queryInstanceList: Query.list(params), queryInstanceTotal: Query.count()]

      } else {
        response.sendError(401)
      }
    }

    def create() {
      if(authService.userInRole("ROLE_ADMIN") ){
        [queryInstance: new Query(params)]
      } else {
        response.sendError(401)
      }
    }

    def save() {
        if(authService.userInRole("ROLE_ADMIN") ){
            def queryInstance = new Query(params)
            if (!queryInstance.save(flush: true)) {
                render(view: "create", model: [queryInstance: queryInstance])
                return
            }

            flash.message = message(code: 'default.created.message', args: [message(code: 'query.label', default: 'Query'), queryInstance.id])
            redirect(action: "show", id: queryInstance.id)
        } else {
            response.sendError(401)
        }
    }

    def show() {
        if(authService.userInRole("ROLE_ADMIN") ){
            def queryInstance = Query.get(params.id)
            if (!queryInstance) {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "list")
                return
            }
            [queryInstance: queryInstance]
        } else {
            response.sendError(401)
        }
    }

    def edit() {
        if(authService.userInRole("ROLE_ADMIN") ){
            def queryInstance = Query.get(params.id)
            if (!queryInstance) {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "list")
                return
            }

            [queryInstance: queryInstance]
        } else {
            response.sendError(401)
        }
    }

    def update() {
        if(authService.userInRole("ROLE_ADMIN") ){
            def queryInstance = Query.get(params.id)
            if (!queryInstance) {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "list")
                return
            }

            if (params.version) {
                def version = params.version.toLong()
                if (queryInstance.version > version) {
                    queryInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
                            [message(code: 'query.label', default: 'Query')] as Object[],
                            "Another user has updated this Query while you were editing")
                    render(view: "edit", model: [queryInstance: queryInstance])
                    return
                }
            }

            queryInstance.properties = params

            if (!queryInstance.save(flush: true)) {
                render(view: "edit", model: [queryInstance: queryInstance])
                return
            }

            flash.message = message(code: 'default.updated.message', args: [message(code: 'query.label', default: 'Query'), queryInstance.id])
            redirect(action: "show", id: queryInstance.id)
        } else {
            response.sendError(401)
        }
    }

    def delete() {
        if(authService.userInRole("ROLE_ADMIN") ){
            def queryInstance = Query.get(params.id)
            if (!queryInstance) {
                flash.message = message(code: 'default.not.found.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "list")
                return
            }

            try {
                queryInstance.delete(flush: true)
                flash.message = message(code: 'default.deleted.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "list")
            }
            catch (DataIntegrityViolationException e) {
                flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'query.label', default: 'Query'), params.id])
                redirect(action: "show", id: params.id)
            }
        } else {
            response.sendError(401)
        }
    }
}
