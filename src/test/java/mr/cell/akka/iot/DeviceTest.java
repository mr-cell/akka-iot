package mr.cell.akka.iot;

import akka.actor.ActorRef;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Created by U517779 on 2017-09-05.
 */
public class DeviceTest extends AbstractIoTTest {

	@Test
	public void testReplyWithEmptyReadingIfNoTemperatureIsKnown() {
		ActorRef device = system.actorOf(Device.props("group", "device"));
		device.tell(new Device.ReadTemperature(42L), probe.getRef());
		Device.RespondTemperature response = probe.expectMsgClass(Device.RespondTemperature.class);
		assertEquals(42L, response.getRequestId());
		assertEquals(Optional.empty(), response.getValue());
	}

	@Test
	public void testReplyWithLatestTemperatureReading() {
		ActorRef device = system.actorOf(Device.props("group", "device"));

		device.tell(new Device.RecordTemperature(1L, 24.0), probe.getRef());
		assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

		device.tell(new Device.ReadTemperature(2L), probe.getRef());
		Device.RespondTemperature response1 = probe.expectMsgClass(Device.RespondTemperature.class);
		assertEquals(2L, response1.getRequestId());
		assertEquals(Optional.of(24.0), response1.getValue());

		device.tell(new Device.RecordTemperature(3L, 55.0), probe.getRef());
		assertEquals(3L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());

		device.tell(new Device.ReadTemperature(4L), probe.getRef());
		Device.RespondTemperature response2 = probe.expectMsgClass(Device.RespondTemperature.class);
		assertEquals(4L, response2.getRequestId());
		assertEquals(Optional.of(55.0), response2.getValue());
	}

	@Test
	public void testReplyToRegistractionRequests() {
		ActorRef device = system.actorOf(Device.props("group", "device"));

		device.tell(new DeviceManager.RequestTrackDevice("group", "device"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		assertEquals(device, probe.getLastSender());
	}

	@Test
	public void testIgnoreWrongRegistrationRequests() {
		ActorRef device = system.actorOf(Device.props("group", "device"));

		device.tell(new DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.getRef());
		probe.expectNoMsg();

		device.tell(new DeviceManager.RequestTrackDevice("group", "wrongDevice"), probe.getRef());
		probe.expectNoMsg();
	}

	@Test
	public void testReturnSameActorForSameDeviceId() {
		ActorRef groupActor = system.actorOf(DeviceGroup.props("group"));

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor1 = probe.getLastSender();

		groupActor.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef deviceActor2 = probe.getLastSender();

		assertEquals(deviceActor1, deviceActor2);
	}
}
