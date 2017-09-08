package mr.cell.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.Optional;

/**
 * Created by U517779 on 2017-09-05.
 */
public class Device extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private final String groupId;
	private final String deviceId;
	private Optional<Double> lastTemperatureReading = Optional.empty();

	public Device(String groupId, String deviceId) {
		this.groupId = groupId;
		this.deviceId = deviceId;
	}

	public static Props props(String groupId, String deviceId) {
		return Props.create(Device.class, groupId, deviceId);
	}

	@Override
	public void preStart() throws Exception {
		log.info("Device {}-{} started.", groupId, deviceId);
	}

	@Override
	public void postStop() throws Exception {
		log.info("Device {}-{} stopped.", groupId, deviceId);
	}

	private void onTrackDevice(DeviceManager.RequestTrackDevice trackMsg) {
		if(!groupId.equals(trackMsg.getGroupId()) || !deviceId.equals(trackMsg.getDeviceId())) {
			log.warning("Ignoring TrackDevice request for {}-{}. This actor is responsible for {}-{}.",
					trackMsg.getGroupId(), trackMsg.getDeviceId(), groupId, deviceId);
			return;
		}

		getSender().tell(new DeviceManager.DeviceRegistered(), getSelf());
	}

	private void onRecordTemperature(RecordTemperature recordMsg) {
		log.info("Recorded temperature reading {} with {}.", recordMsg.getValue(), recordMsg.getRequestId());
		lastTemperatureReading = Optional.of(recordMsg.getValue());
		getSender().tell(new TemperatureRecorded(recordMsg.getRequestId()), getSelf());
	}

	private void onReadTemperature(ReadTemperature readMsg) {
		getSender().tell(new RespondTemperature(readMsg.getRequestId(), lastTemperatureReading), getSelf());
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
				.match(RecordTemperature.class, this::onRecordTemperature)
				.match(ReadTemperature.class, this::onReadTemperature)
				.build();
	}

	public static final class ReadTemperature {
		private final long requestId;

		public ReadTemperature(long requestId) {
			this.requestId = requestId;
		}

		public long getRequestId() {
			return requestId;
		}
	}

	public static final class RespondTemperature {

		private final long requestId;
		private final Optional<Double> value;

		public RespondTemperature(long requestId, Optional<Double> value) {
			this.requestId = requestId;
			this.value = value;
		}

		public Optional<Double> getValue() {
			return value;
		}

		public long getRequestId() {
			return requestId;
		}
	}

	public static final class RecordTemperature {
		private final long requestId;
		private final double value;

		public RecordTemperature(long requestId, double value) {
			this.requestId = requestId;
			this.value = value;
		}

		public long getRequestId() {
			return requestId;
		}

		public double getValue() {
			return value;
		}
	}

	public static final class TemperatureRecorded {
		private final long requestId;

		public TemperatureRecorded(long requestId) {
			this.requestId = requestId;
		}

		public long getRequestId() {
			return requestId;
		}
	}
}
