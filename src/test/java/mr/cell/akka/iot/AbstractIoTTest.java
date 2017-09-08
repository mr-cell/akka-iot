package mr.cell.akka.iot;

import akka.actor.ActorSystem;
import akka.testkit.javadsl.TestKit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * Created by U517779 on 2017-09-06.
 */
public abstract class AbstractIoTTest {

	protected static ActorSystem system;

	protected TestKit probe;

	@BeforeClass
	public static void setupBeforeClass() {
		system = ActorSystem.create();
	}

	@Before
	public void setup() {
		probe = new TestKit(system);
	}

	@AfterClass
	public static void terminate() {
		TestKit.shutdownActorSystem(system);
		system = null;
	}
}
