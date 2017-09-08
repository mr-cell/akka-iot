package mr.cell.akka.iot;

/**
 * Created by U517779 on 2017-09-08.
 */
public abstract class AbstractNoBodyMessage {

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return this.getClass().getName().hashCode();
	}
}
