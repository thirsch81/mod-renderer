package de.thhi.soapmock.test.unit;

import static org.junit.Assert.*;

import de.thhi.vertx.renderer.RenderVerticle
import org.junit.Test;
import org.vertx.groovy.core.Vertx

class RenderVerticleTest {

	@Test
	public void testRender() {
		def renderer = new RenderVerticle()
		renderer.templates = { ["name" :'${content}'] }

		assertEquals( ["status": "ok", "name": "testContent"].toString() , renderer.render("name", ["content": "testContent"]).toString() )
	}
}
