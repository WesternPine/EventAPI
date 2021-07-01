package dev.westernpine.eventapi.objects;

public interface Cancellable {
	
	/**
	 * @return True if the event is cancelled, false if it isn't.
	 */
	public boolean isCancelled();
	
	/**
	 * Set the cancelled status of an event.
	 * @param cancelled The new status of the event.
	 */
	public void setCancelled(boolean cancelled);

}
