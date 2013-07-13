package thhi.vertx.renderer

import groovy.text.SimpleTemplateEngine

import org.vertx.groovy.platform.Verticle

public class RenderVerticle extends Verticle {

	def start() {
		readTemplates()
		vertx.eventBus.registerHandler("renderer.render", handleRender)
		vertx.eventBus.registerHandler("renderer.templates", handleTemplates)
		logInfo("RenderVerticle started")
	}

	def templates = {
		vertx.sharedData.getMap("rendererTemplates")
	}

	def error(message) {
		["status": "error", "message": message]
	}

	def fetchAllOk(templates) {
		["status": "ok", "templates": templates]
	}

	def fetchOk(template) {
		["status": "ok", "template": template]
	}

	def renderOk(name, response) {
		["status": "ok", "${name}": response]
	}

	def submitOk() {
		["status": "ok"]
	}

	def handleRender =  { message ->

		def body = message.body
		logDebug("Received ${body}")

		if(!("name" in body && "binding" in body)) {
			replyErrorTo(message, "Expected message format [name: <name>, binding: <binding>], not " + body)
			return
		}

		message.reply(render(body.name, body.binding))
	}

	def handleTemplates = { message ->

		def body = message.body
		logDebug("Received ${body}")

		if(!("action" in body)) {
			replyErrorTo(message, "Expected message format [action: <action>, (name: <name>), (template: <template>)], not " + body)
			return
		}

		switch (body.action) {

			case "fetch":

				if("name" in body) {
					def result = templates()[body.name]
					message.reply(fetchOk(result))
					logDebug("Sent template " + body.name + " to client")
					break
				}
				def result = [];
				templates().each { k, v ->
					result.add(["name": k, "template": v])
				}
				message.reply(fetchAllOk(result))
				logDebug("Sent ${result.size()} templates to client")
				break

			case "submit":

				if(!(body.containsKey("name") && body.containsKey("template"))) {
					replyErrorTo(message, "Expected message format [action: 'submit', name: <name>, template: <template>], not " + body)
					break
				}

				if(!(body.name ==~ /^[a-zA-Z0-9_-]{3,25}$/)) {
					replyErrorTo(message, "Expected proper template name, not " + body.name)
					break
				}
				templates()[body.name] = body.template
				logDebug("Accepted template " + body.name + " from client")
				message.reply(submitOk())
				break

			default:
				replyErrorTo(message, "Unknown action: expected: fetch|submit, not " + body.action)
		}
	}

	def render(name, binding) {

		try {

			def r_time = now()
			def response = new SimpleTemplateEngine().createTemplate(templates()[name]).make(binding).toString()
			logDebug("Rendered template ${name} for binding ${binding} in ${now() - r_time}ms")
			renderOk(name, response)
		} catch (Exception e) {

			logError("Exception when rendering ${name} for binding ${binding}: ", e)
			error(e.message)
		}
	}

	def readTemplates() {
		vertx.fileSystem.readDir("templates", ".*\\.template") { result ->
			if (result.succeeded) {
				readTemplateDirectory(result.result)
			} else {
				logError("No template directory found")
			}
		}
	}

	def readTemplateDirectory(files) {
		if(files) {
			logDebug("Reading templates...")
			for (file in files) {
				readTemplateFile(file)
			}
		} else {
			logError("No templates to read")
		}
	}

	def readTemplateFile(file) {
		vertx.fileSystem.readFile(file) { result ->
			if (result.succeeded) {
				templates()[new File(file).name - ".template"] = result.result.toString()
				logDebug("Read template ${file}")
			} else {
				logError("Error reading template ${file}", result.cause)
			}
		}
	}

	def replyErrorTo(message, text) {
		logError(text)
		message.reply(error(text))
	}

	def logInfo(msg, err = null) {
		if(container.logger.infoEnabled) {
			container.logger.info(msg, err)
		}
	}

	def logError(msg, err = null) {
		container.logger.error(msg, err)
	}

	def logDebug(msg, err = null) {
		if(container.logger.debugEnabled) {
			container.logger.debug(msg, err)
		}
	}

	def now = { System.currentTimeMillis() }
}