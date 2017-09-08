package mr.cell.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.IOException;

/**
 * Created by U517779 on 2017-09-05.
 */
public class IoTSupervisor extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private ActorRef deviceManager;
	private ActorRef dashboardManager;

	public static Props props() {
		return Props.create(IoTSupervisor.class);
	}

	@Override
	public void preStart() throws Exception {
		log.info("IoT Application started");

		deviceManager = getContext().actorOf(Props.create(DeviceManager.class), "device-manager");
		dashboardManager = getContext().actorOf(Props.create(DashboardManager.class), "dashboard-manager");
	}

	@Override
	public void postStop() throws Exception {
		log.info("IoT Application stopped");
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.build();
	}

	public static void main(String[] args) throws IOException {
		ActorSystem system = ActorSystem.create("iot-web");

		try {
			ActorRef iotSupervisor = system.actorOf(IoTSupervisor.props(), "iot-web-supervisor");

			System.out.println("Press ENTER to exit the system");
			System.in.read();
		} finally {
			system.terminate();
		}
	}
}
