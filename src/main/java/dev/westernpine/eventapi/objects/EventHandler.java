package dev.westernpine.eventapi.objects;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import dev.westernpine.eventapi.describers.EventExecution;

@Retention(RetentionPolicy.RUNTIME) //Needed at runtime to determine what methods are annotated using reflections.
@Target(ElementType.METHOD)
@Documented
public @interface EventHandler {

	/**
	 * The EventExecution specifies to the caller the execution order that the event is specifying it should be executed in.
	 * @return The preffered execution order.
	 */
	public EventExecution execution() default EventExecution.MIDDLE;
	
	/**
	 * This specifies to the caller whether it should ignore cancelled events or not.
	 * @return True to be executed on cancelled events, False to not be executed on cancelled events.
	 */
	public boolean ignoreCancelled() default false;
	
	

}
