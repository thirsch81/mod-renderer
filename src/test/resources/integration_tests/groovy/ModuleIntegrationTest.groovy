/*
 * Example Groovy integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */

import static org.vertx.testtools.VertxAssert.*

// And import static the VertxTests script
import org.vertx.groovy.testtools.VertxTests;

// The test methods must being with "test"

def testModuleRender() {
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

// Make sure you initialize
VertxTests.initialize(this)

container.deployModule("thhi.vertx~renderer~0.6.0", { result ->
	// Deployment is asynchronous and this handler will be called when it's complete (or failed)
	assertTrue(result.cause(), result.succeeded)
	assertNotNull("deploymentID should not be null", result.result())
	// If deployed correctly then start the tests!
	VertxTests.startTests(this)
})



