package mr.cell.akka.iot;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Created by U517779 on 2017-09-06.
 */
public class DeviceManagerTest extends AbstractIoTTest {

	@Test
	public void testRegisterDeviceGroupAndDeviceActor() {
		ActorRef managerActor = system.actorOf(DeviceManager.props());

		managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor1 = probe.getLastSender();

		managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor2 = probe.getLastSender();
		assertNotEquals(deviceActor1, deviceActor2);

		deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), probe.getRef());
		assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
		deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), probe.getRef());
		assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
	}

	@Test
	public void testDeviceGroupsShutDown() {
		ActorRef managerActor = system.actorOf(DeviceManager.props());

		Set<String> deviceIds = Stream.of("device1", "device2").collect(Collectors.toSet());

		managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef toShutDown = probe.getLastSender();

		managerActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);

		probe.watch(toShutDown);
		toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toShutDown);
	}
}
