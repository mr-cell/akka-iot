package mr.cell.akka.iot;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.testkit.javadsl.TestKit;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by U517779 on 2017-09-08.
 */
public class DeviceGroupQueryTest extends AbstractIoTTest {

	private void assertEqualTemperatures(Map<String, DeviceGroup.TemperatureReading> expectedTemperatures, Map<String, DeviceGroup.TemperatureReading> receivedTemperatures) {
		boolean allMatched = expectedTemperatures.entrySet().stream()
				.allMatch(entry -> {
					DeviceGroup.TemperatureReading expected = entry.getValue();
					DeviceGroup.TemperatureReading received = receivedTemperatures.get(entry.getKey());
					return expected.equals(received);
				});
		assertTrue(allMatched);
	}

	@Test
	public void testReturnTemperatureValueForWorkingDevices() {
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.getRef(), "device1");
		actorToDeviceId.put(device2.getRef(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				1L,
				probe.getRef(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).getRequestId());
		assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).getRequestId());

		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());

		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
		assertEquals(1L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

	@Test
	public void testReturnTemperatureNotAvailableForDevicesWithNoReadings() {
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.getRef(), "device1");
		actorToDeviceId.put(device2.getRef(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				1L,
				probe.getRef(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).getRequestId());
		assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).getRequestId());

		queryActor.tell(new Device.RespondTemperature(0L, Optional.empty()), device1.getRef());
		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());

		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
		assertEquals(1L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.TemperatureNotAvailable());
		expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

	@Test
	public void testReturnDeviceNotAvailableIfDeviceStopsBeforeAnswering() {
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.getRef(), "device1");
		actorToDeviceId.put(device2.getRef(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				1L,
				probe.getRef(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).getRequestId());
		assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).getRequestId());

		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
		device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
		assertEquals(1L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceGroup.DeviceNotAvailable());

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

	@Test
	public void testReturnTemperatureReadingEvenIfDeviceStopsAfterAnswering() {
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.getRef(), "device1");
		actorToDeviceId.put(device2.getRef(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				1L,
				probe.getRef(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).getRequestId());
		assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).getRequestId());

		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());
		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(2.0)), device2.getRef());
		device2.getRef().tell(PoisonPill.getInstance(), ActorRef.noSender());

		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
		assertEquals(1L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

	@Test
	public void testReturnDeviceTimeoutIfDeviceDoesNotAnswerInTime() {
		TestKit device1 = new TestKit(system);
		TestKit device2 = new TestKit(system);

		Map<ActorRef, String> actorToDeviceId = new HashMap<>();
		actorToDeviceId.put(device1.getRef(), "device1");
		actorToDeviceId.put(device2.getRef(), "device2");

		ActorRef queryActor = system.actorOf(DeviceGroupQuery.props(
				actorToDeviceId,
				1L,
				probe.getRef(),
				new FiniteDuration(3, TimeUnit.SECONDS)));

		assertEquals(0L, device1.expectMsgClass(Device.ReadTemperature.class).getRequestId());
		assertEquals(0L, device2.expectMsgClass(Device.ReadTemperature.class).getRequestId());

		queryActor.tell(new Device.RespondTemperature(0L, Optional.of(1.0)), device1.getRef());

		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(
				FiniteDuration.create(5, TimeUnit.SECONDS),
				DeviceGroup.RespondAllTemperatures.class);
		assertEquals(1L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceGroup.DeviceTimeout());

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

	@Test
	public void testCollectTemperaturesFromAllActiveDevices() {
		ActorRef deviceGroup = system.actorOf(DeviceGroup.props("group"), "group");

		deviceGroup.tell(new DeviceManager.RequestTrackDevice("group", "device1"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef device1 = probe.getLastSender();

		deviceGroup.tell(new DeviceManager.RequestTrackDevice("group", "device2"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef device2 = probe.getLastSender();

		deviceGroup.tell(new DeviceManager.RequestTrackDevice("group", "device3"), probe.getRef());
		probe.expectMsgClass(DeviceManager.DeviceRegistered.class);
		ActorRef device3 = probe.getLastSender();

		device1.tell(new Device.RecordTemperature(0L, 1.0), probe.getRef());
		assertEquals(0L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
		device2.tell(new Device.RecordTemperature(1L, 2.0), probe.getRef());
		assertEquals(1L, probe.expectMsgClass(Device.TemperatureRecorded.class).getRequestId());
		// No temperature for device 3

		deviceGroup.tell(new DeviceGroup.RequestAllTemperatures(0L), probe.getRef());
		DeviceGroup.RespondAllTemperatures response = probe.expectMsgClass(DeviceGroup.RespondAllTemperatures.class);
		assertEquals(0L, response.getRequestId());

		Map<String, DeviceGroup.TemperatureReading> expectedTemperatures = new HashMap<>();
		expectedTemperatures.put("device1", new DeviceGroup.Temperature(1.0));
		expectedTemperatures.put("device2", new DeviceGroup.Temperature(2.0));
		expectedTemperatures.put("device3", new DeviceGroup.TemperatureNotAvailable());

		assertEqualTemperatures(expectedTemperatures, response.getTemperatures());
	}

}
