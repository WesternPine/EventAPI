package dev.westernpine.eventapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.westernpine.eventapi.describers.EventExecution;
import dev.westernpine.eventapi.objects.Cancellable;
import dev.westernpine.eventapi.objects.Event;
import dev.westernpine.eventapi.objects.EventHandler;
import dev.westernpine.eventapi.objects.Listener;

public class EventManager {
	
	private Map<Listener, List<Method>> eventHandlers;
	
	/**
	 * The event manager class that manages listeners and events.
	 */
	public EventManager() {
		eventHandlers = new HashMap<>();
	}
	
	/**
	 * Register listener classes containing EventHandler methods used to handle events. EventHandler methods MUST have ONE parameter, whose type is assignable from the Event classs.
	 * @param listeners Classes containing EventHandler methods used to handle events.
	 */
	public void registerListeners(Listener...listeners) {
		if(listeners == null || listeners.length == 0)
			return;
		
		for(Listener listener : listeners) {
			if(listener == null)
				continue;
			List<Method> handlers = new ArrayList<>();
			for(Method method : listener.getClass().getDeclaredMethods()) {
				if(method.isAnnotationPresent(EventHandler.class)) {
					if(method.getParameters().length == 1) {
						if(Event.class.isAssignableFrom(method.getParameterTypes()[0])) {
							method.setAccessible(true);
							handlers.add(method);
						}
					}
				}
			}
			eventHandlers.put(listener, handlers);
		}
	}

	/**
	 * Unregister any registered listener classes
	 * @param listeners Classes containing EventHandler methods used to handle events, that have been previously registered.
	 */
	public void unregisterListeners(Listener...listeners) {
		if(listeners == null || listeners.length == 0)
			return;
		
		for(Listener listener : listeners) {
			if(listener == null)
				continue;
			eventHandlers.remove(listener);
		}
	}
	
	/**
	 * Call all event handlers listeneing for the specific event with the specified event object. EventHandlers are executed in the following order: 
	 * 
	 * For every event that is called, all listeners listening for the "Event" event, will be called first.
	 * All "Event" listeners will be called in the order of the EventExecution property specified.
	 * If an event is cancelled, only listeners containing the "true" value for "ignoreCancelled" will be called.
	 * If an event is uncancelled, only the rest of the events that haven't been reviewed for execution yet will be called. (Make use of EventExecution to get around any issues here.)
	 * 
	 * After all "Event" EventHandlers have been called, if the event in question isn't an "Event", then all other EventHandlers will be called with the same properties as specified for the "Event" EventHandlers.
	 * 
	 * @param <T> The event extending the Event class.
	 * @param event The event to call.
	 */
	public <T extends Event> void call(T event) {
		Map<Listener, List<Method>> listeners = null;
		synchronized(eventHandlers) {
			listeners = new HashMap<>(eventHandlers);
		}
		
		BiFunction<Method, EventExecution, Boolean> decider = (method, execution) -> {
			EventHandler handler = method.getDeclaredAnnotationsByType(EventHandler.class)[0];
			if (handler.execution().equals(execution)) {
				if (event instanceof Cancellable && !handler.ignoreCancelled()) {
					if (!((Cancellable) event).isCancelled()) {
						return true;
					}
				} else {
					return true;
				}
			}
			return false;
		};
		
		listeners.entrySet().forEach(entry -> {
			final Listener listener = entry.getKey();
			List<Method> methods = entry.getValue().stream()
					.filter(method -> method.getParameterTypes()[0].equals(Event.class) || method.getParameterTypes()[0].isAssignableFrom(event.getClass()))
					.collect(Collectors.toList());
			
			Consumer<Method> invoker = method -> {
				try {
					method.invoke(listener, event);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("An EventHandler ecountered an unhandled exception durring it's execution!");
				}
			};
			
			BiConsumer<List<Method>, Predicate<Method>> accepter = (allMethods, predicate) -> {
				Stream.of(EventExecution.values()).forEachOrdered(execution -> {
					allMethods.stream()
					.filter(predicate)
					.forEach(method -> {if(decider.apply(method, execution))invoker.accept(method);});
				});
			};
	
			accepter.accept(methods, method -> method.getParameterTypes()[0].equals(Event.class));
			if(event.getClass() != Event.class) {
				accepter.accept(methods, method -> !method.getParameterTypes()[0].equals(Event.class) && method.getParameterTypes()[0].isAssignableFrom(event.getClass()));
			}
		});
		
	}

}
