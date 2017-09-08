package mr.cell.akka.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by U517779 on 2017-09-05.
 */
public class DeviceManager extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

	private final Map<String, ActorRef> groupIdsToActors;
	private final Map<ActorRef, String> actorsToGroupIds;

	public static Props props() {
		return Props.create(DeviceManager.class);
	}

	public DeviceManager() {
		groupIdsToActors = new HashMap<>();
		actorsToGroupIds = new HashMap<>();
	}

	@Override
	public void preStart() throws Exception {
		log.info("DeviceManager started.");
	}

	@Override
	public void postStop() throws Exception {
		log.info("DeviceManager stopped.");
	}

	private void onTrackDevice(RequestTrackDevice trackMsg) {
		String groupId = trackMsg.getGroupId();

		ActorRef deviceGroup = groupIdsToActors.get(groupId);
		if(deviceGroup == null) {
			deviceGroup = getContext().actorOf(DeviceGroup.props(groupId), "group-" + groupId);
			getContext().watch(deviceGroup);
			groupIdsToActors.put(groupId, deviceGroup);
			actorsToGroupIds.put(deviceGroup, groupId);
		}
		deviceGroup.forward(trackMsg, getContext());
	}

	private void onTerminated(Terminated t) {
		ActorRef group = t.getActor();
		String groupId = actorsToGroupIds.get(group);
		log.info("Device group actor for {} has been terminated", groupId);
		groupIdsToActors.remove(groupId);
		actorsToGroupIds.remove(group);
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder()
				.match(RequestTrackDevice.class, this::onTrackDevice)
				.match(Terminated.class, this::onTerminated)
				.build();
	}

	public static final class RequestTrackDevice {
		private final String groupId;
		private final String deviceId;

		public RequestTrackDevice(String groupId, String deviceId) {
			this.groupId = groupId;
			this.deviceId = deviceId;
		}

		public String getGroupId() {
			return groupId;
		}

		public String getDeviceId() {
			return deviceId;
		}
	}

	public static final class DeviceRegistered extends AbstractNoBodyMessage { }
}
