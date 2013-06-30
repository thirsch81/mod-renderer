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

			def errorMsg = "Expected message format [name: <name>, binding: <binding>], not ${body.toString()}"
			logError(errorMsg)
			message.reply(error(errorMsg))
		} else {

			message.reply(render(body.name, body.binding))
		}
	}

	def handleTemplates = { message ->

		def body = message.body
		logDebug("Received ${body}")

		if(!("action" in body)) {

			def errorMsg = "Expected message format [action: <action>, (name: <name>), (template: <template>)], not ${body.toString()}"
			logError(errorMsg)
			message.reply(error(errorMsg))
		} else {

			switch (body.action) {

				case "fetch":

					if("name" in body) {

						def result = templates()[body.name]
						message.reply(fetchOk(result))
						logDebug("Sent template ${body.name} to client")
					} else {

						def result = [];
						templates().each { k, v ->
							result.add(["name": k, "template": v])
						}
						message.reply(fetchAllOk(result))
						logDebug("Sent ${result.size()} templates to client")
					}
					break

				case "submit":

					if(!("name" in body && "template" in body)) {

						def errorMsg = "Expected message format [action: 'submit', name: <name>, template: <template>], not ${body.toString()}"
						logError(errorMsg)
						message.reply(error(errorMsg))
					} else {
						templates()[body.name] = body.template
						logDebug("Accepted template ${body.name} from client")
						message.reply(submitOk())
					}
					break

				default:
					def errorMsg = "Unknown action: expected: fetch|submit, not ${body.action}"
					logError(errorMsg)
					message.reply(error(errorMsg))
			}
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

	def now = { System.currentTimeMillis() }

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
}