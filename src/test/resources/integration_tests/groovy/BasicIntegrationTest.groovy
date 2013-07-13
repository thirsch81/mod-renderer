import static org.vertx.testtools.VertxAssert.*

import org.vertx.groovy.testtools.VertxTests

import thhi.vertx.renderer.RenderVerticle
// The test methods must being with "test"

def testDeployRenderVerticle() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		testComplete()
	}
}

def testRenderVerticleRender() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.render", ["name" : "test-response", "binding" : ["content" : "some content"]]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			reply.body.with {
				assertEquals("ok", it.status)
				assertNotNull(it."test-response")
				assertTrue(it."test-response".contains('<?xml version="1.0" encoding="UTF-8" ?>'))
				assertTrue(it."test-response".contains("some content"))
			}
			testComplete()
		}
	}
}

def testRenderVerticleSubmitTemplate() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["action" : "submit", "name" : "test_submit", "template" : "test-submit-template"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			assertEquals("ok", reply.body.status)
			assertNotNull(vertx.sharedData.getMap("rendererTemplates")["test_submit"])
			assertEquals("test-submit-template", vertx.sharedData.getMap("rendererTemplates")["test_submit"])
			testComplete()
		}
	}
}

def testRenderVerticleUnknownAction() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["action" : "unknown"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			assertEquals("error", reply.body.status)
			assertEquals("Unknown action: expected: fetch|submit, not unknown", reply.body.message)
			testComplete()
		}
	}
}

def testRenderVerticleNoAction() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["no-action" : "here"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			assertEquals("error", reply.body.status)
			assertEquals("Expected message format [action: <action>, (name: <name>), (template: <template>)], not [no-action:here]", reply.body.message)
			testComplete()
		}
	}
}

def testRenderVerticleFetchTemplate() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["action" : "fetch", "name" : "test-response"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			reply.body.with {
				assertEquals("ok", it.status)
				assertNotNull(it.template)
				assertEquals('<?xml version="1.0" encoding="UTF-8" ?><response>${content}</response>', it.template)
			}
			testComplete()
		}
	}
}

def testRenderVerticleFetchAllTemplates() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["action" : "fetch"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			reply.body.with {
				assertEquals("ok", it.status)
				assertNotNull(it.templates)
				assertTrue("test-response" in it.templates)
			}
			testComplete()
		}
	}
}

VertxTests.initialize(this)
VertxTests.startTests(this)