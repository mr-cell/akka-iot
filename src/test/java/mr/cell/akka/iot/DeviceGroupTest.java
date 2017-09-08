package mr.cell.akka.iot;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import org.junit.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * Created by U517779 on 2017-09-05.
 */
public class DeviceGroupTest extends AbstractIoTTest {

	@Test
	public void testRegisterDeviceActor() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor1 = probe.getLastSender();

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor2 = probe.getLastSender();
		assertNotEquals(deviceActor1, deviceActor2);

		deviceActor1.tell(new Device.RecordTemperature(0L, 1.0), probe.getRef());
		assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
		deviceActor2.tell(new Device.RecordTemperature(1L, 2.0), probe.getRef());
		assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
	}

	@Test
	public void testIgnoreRequestsForWrongGroupId() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new DeviceManager.RequestTrackDevice("wrongGroup", "device1"), probe.getRef());
		probe.expectNoMsg();
	}

	@Test
	public void testListActiveDevices() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		Set<String> deviceIds = Stream.of("device1", "device2").collect(Collectors.toSet());
		deviceIds.stream().forEach(deviceId -> {
			groupActor.tell(new DeviceManager.RequestTrackDevice("group", deviceId), probe.getRef());
			probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		});

		groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.getRef());
		DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
		assertEquals(0L, reply.getRequestId());
		assertEquals(deviceIds, reply.getDeviceIds());
	}

	@Test
	public void testListActiveDevicesAfterOneShutsDown() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		Set<String> deviceIds = Stream.of("device1", "device2").collect(Collectors.toSet());

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef toShutDown = probe.getLastSender();

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);


		groupActor.tell(new DeviceGroup.RequestDeviceList(0L), probe.getRef());
		DeviceGroup.ReplyDeviceList reply = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
		assertEquals(0L, reply.getRequestId());
		assertEquals(deviceIds, reply.getDeviceIds());

		probe.watch(toShutDown);
		toShutDown.tell(PoisonPill.getInstance(), ActorRef.noSender());
		probe.expectTerminated(toShutDown);

		probe.awaitAssert(() -> {
			groupActor.tell(new DeviceGroup.RequestDeviceList(1L), probe.getRef());
			DeviceGroup.ReplyDeviceList r = probe.expectMsgClass(DeviceGroup.ReplyDeviceList.class);
			assertEquals(1L, r.getRequestId());
			assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.getDeviceIds());
			return null;
		});
	}
}
