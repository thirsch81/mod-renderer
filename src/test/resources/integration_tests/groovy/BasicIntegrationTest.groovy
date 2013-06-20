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

def testRenderVerticleFetchTemplates() {
	container.deployWorkerVerticle("groovy:" + RenderVerticle.class.name) { result ->
		assertTrue("${result.cause()}", result.succeeded)
		vertx.eventBus.send("renderer.templates", ["action" : "fetch"]) { reply ->
			assertNotNull(reply)
			container.logger.info(reply.body)
			reply.body.with {
				assertEquals("ok", it.status)
				assertNotNull(it.templates)
				assertEquals(
						["name": "test-response", "template": '<?xml version="1.0" encoding="UTF-8" ?><response>${content}</response>'], it.templates[0])
			}
			testComplete()
		}
	}
}

VertxTests.initialize(this)
VertxTests.startTests(this)