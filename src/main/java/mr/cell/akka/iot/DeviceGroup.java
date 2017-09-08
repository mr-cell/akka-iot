package mr.cell.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by U517779 on 2017-09-05.
 */
public class DeviceGroup extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private String groupId;
	private final Map<String, ActorRef> deviceIdToActor;
	private final Map<ActorRef, String> actorToDeviceId;

	public static Props props(String groupId) {
		return Props.create(DeviceGroup.class, groupId);
	}

	public DeviceGroup(String groupId) {
		this.groupId = groupId;
		deviceIdToActor = new HashMap<>();
		actorToDeviceId = new HashMap<>();
	}

	@Override
	public void preStart() throws Exception {
		log.info("DeviceGroup {} started.", groupId);
	}

	@Override
	public void postStop() throws Exception {
		log.info("DeviceGroup {} stopped.", groupId);
	}

	private void onTrackDevice(DeviceManager.RequestTrackDevice trackMsg) {
		if(!groupId.equals(trackMsg.getGroupId())) {
			log.warning("Ignoring TrackDevice request for {}. This actor is responsible for {}.",
					trackMsg.getGroupId(), groupId);
			return;
		}

		ActorRef device = deviceIdToActor.get(trackMsg.getDeviceId());
		if(device == null) {
			log.info("Creating device actor for {}.", trackMsg.getDeviceId());
			device = getContext().actorOf(Device.props(groupId, trackMsg.getDeviceId()), "device-" + groupId + "-" + trackMsg.getDeviceId());
			getContext().watch(device);
			deviceIdToActor.put(trackMsg.getDeviceId(), device);
			actorToDeviceId.put(device, trackMsg.getDeviceId());
		}

		device.forward(trackMsg, getContext());
	}

	private void onDeviceList(RequestDeviceList deviceListMsg) {
		getSender().tell(new ReplyDeviceList(deviceListMsg.getRequestId(), deviceIdToActor.keySet()), getSelf());
	}

	private void onTerminated(Terminated terminatedMsg) {
		ActorRef deviceActor = terminatedMsg.getActor();
		String deviceId = actorToDeviceId.get(deviceActor);
		log.info("Device actor for {} has been terminated.", deviceId);
		deviceIdToActor.remove(deviceId);
		actorToDeviceId.remove(deviceActor);
	}

	private void onRequestAllTemperatures(RequestAllTemperatures rat) {
		getContext().actorOf(DeviceGroupQuery.props(actorToDeviceId, rat.getRequestId(), getSender(), new FiniteDuration(3, TimeUnit.SECONDS)));
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
				.match(RequestDeviceList.class, this::onDeviceList)
				.match(RequestAllTemperatures.class, this::onRequestAllTemperatures)
				.match(Terminated.class, this::onTerminated)
				.build();
	}

	public static final class RequestDeviceList {
		private final long requestId;

		public RequestDeviceList(long requestId) {
			this.requestId = requestId;
		}

		public long getRequestId() {
			return requestId;
		}
	}

	public static final class ReplyDeviceList {
		private final long requestId;
		private final Set<String> deviceIds;

		public ReplyDeviceList(long requestId, Set<String> deviceIds) {
			this.requestId = requestId;
			this.deviceIds = deviceIds;
		}

		public long getRequestId() {
			return requestId;
		}

		public Collection<String> getDeviceIds() {
			return deviceIds;
		}
	}

	public static final class RequestAllTemperatures {
		private final long requestId;

		public RequestAllTemperatures(long requestId) {
			this.requestId = requestId;
		}

		public long getRequestId() {
			return requestId;
		}
	}

	public static final class RespondAllTemperatures {
		private final long requestId;
		private final Map<String, TemperatureReading> temperatures;

		public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatues) {
			this.requestId = requestId;
			this.temperatures = temperatues;
		}

		public long getRequestId() {
			return requestId;
		}

		public Map<String, TemperatureReading> getTemperatures() {
			return temperatures;
		}
	}

	public static interface TemperatureReading { }

	public static final class Temperature implements TemperatureReading {
		public final double value;

		public Temperature(double value) {
			this.value = value;
		}

		public double getValue() {
			return value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			Temperature that = (Temperature) o;

			return Double.compare(that.value, value) == 0;
		}

		@Override
		public int hashCode() {
			long temp = Double.doubleToLongBits(value);
			return (int) (temp ^ (temp >>> 32));
		}
	}

	public static final class TemperatureNotAvailable extends AbstractNoBodyMessage implements TemperatureReading { }

	public static final class DeviceNotAvailable extends AbstractNoBodyMessage implements TemperatureReading { 	}

	public static final class DeviceTimeout extends AbstractNoBodyMessage implements TemperatureReading { }
}
