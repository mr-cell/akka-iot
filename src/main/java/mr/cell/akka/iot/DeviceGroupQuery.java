package mr.cell.akka.iot;

import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by U517779 on 2017-09-08.
 */
public class DeviceGroupQuery extends AbstractActor {
	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private final Map<ActorRef, String> actorToDeviceId;
	private final long requestId;
	private final ActorRef requester;

	private Cancellable queryTimeoutTimer;

	public static Props props(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
		return Props.create(DeviceGroupQuery.class, actorToDeviceId, requestId, requester, timeout);
	}

	public DeviceGroupQuery(Map<ActorRef, String> actorToDeviceId, long requestId, ActorRef requester, FiniteDuration timeout) {
		this.actorToDeviceId = actorToDeviceId;
		this.requestId = requestId;
		this.requester = requester;

		queryTimeoutTimer = getContext().getSystem().scheduler().scheduleOnce(timeout, getSelf(), new CollectionTimeout(), getContext().dispatcher(), getSelf());
	}

	@Override
	public void preStart() throws Exception {
		log.info("DeviceGroupQuery actor started.");
		actorToDeviceId.keySet().stream().forEach(device -> {
			getContext().watch(device);
			device.tell(new Device.ReadTemperature(0L), getSelf());
		});
	}

	@Override
	public void postStop() throws Exception {
		queryTimeoutTimer.cancel();
		log.info("DeviceGroupQuery actor stopped.");
	}

	@Override
	public Receive createReceive() {
		return waitingForReplies(new HashMap<>(), actorToDeviceId.keySet());
	}

	private Receive waitingForReplies(Map<String, DeviceGroup.TemperatureReading> repliesSoFar, Set<ActorRef> stillWaiting) {
		return receiveBuilder()
				.match(Device.RespondTemperature.class, r -> this.onRespondTemperature(r, stillWaiting, repliesSoFar))
				.match(Terminated.class, t -> onTerminated(t, stillWaiting, repliesSoFar))
				.match(CollectionTimeout.class, t -> onCollectionTimeout(t, stillWaiting, repliesSoFar))
				.build();
	}

	private void onRespondTemperature(Device.RespondTemperature response, Set<ActorRef> stillWaiting, Map<String, DeviceGroup.TemperatureReading> repliesSoFar) {
		ActorRef deviceActor = getSender();
		DeviceGroup.TemperatureReading reading = response.getValue()
				.map(value -> (DeviceGroup.TemperatureReading) new DeviceGroup.Temperature(value))
				.orElse(new DeviceGroup.TemperatureNotAvailable());
		receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar);
	}

	private void onTerminated(Terminated terminated, Set<ActorRef> stillWaiting, Map<String, DeviceGroup.TemperatureReading> repliesSoFar) {
		receivedResponse(getSender(), new DeviceGroup.DeviceNotAvailable(), stillWaiting, repliesSoFar);
	}

	private void onCollectionTimeout(CollectionTimeout timeout, Set<ActorRef> stillWaiting, Map<String, DeviceGroup.TemperatureReading> repliesSoFar) {
		Map<String, DeviceGroup.TemperatureReading> replies = new HashMap<>(repliesSoFar);
		stillWaiting.stream().forEach(device -> {
			String deviceId = actorToDeviceId.get(device);
			replies.put(deviceId, new DeviceGroup.DeviceTimeout());
		});
		requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, replies), getSelf());
		getContext().stop(getSelf());
	}

	private void receivedResponse(ActorRef device, DeviceGroup.TemperatureReading reading, Set<ActorRef> stillWaiting, Map<String, DeviceGroup.TemperatureReading> repliesSoFar) {
		getContext().unwatch(device);
		String deviceId = actorToDeviceId.get(device);

		Set<ActorRef> newStillWaiting = new HashSet<>(stillWaiting);
		Map<String, DeviceGroup.TemperatureReading> newRepliesSoFar = new HashMap<>(repliesSoFar);

		newRepliesSoFar.put(deviceId, reading);
		newStillWaiting.remove(device);

		if(newStillWaiting.isEmpty()) {
			requester.tell(new DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar), getSelf());
			getContext().stop(getSelf());
			return;
		} else {
			getContext().become(waitingForReplies(newRepliesSoFar, newStillWaiting));
		}
	}

	public static final class CollectionTimeout { }
}
