# EventAPI
An event system API designed for projects aiming to utilize event-driven development.

# JitPack [![](https://jitpack.io/v/WesternPine/EventAPI.svg)](https://jitpack.io/#WesternPine/EventAPI)
Click the icon above to integrate with your code!

# Requirements
 - Java 8+

# Usage
Usage of the event system is very simple. If you don't understand something, everything is documented, so just follow the documentation as needed.

To get started, you will first need to create an instance of the EventManager class. This will call all it's associated registered listeners for that EventManager. Yes, you can have multiple EventManagers. No, calling an event in one manager will not register listeners in another manager.

`EventManager manager = new EventManager();`

Obviously you will need to make your own events in order for this to serve it's purpose. All you need to do, is make a class that extends the Event class. It is also optional to have a class implement Cancellable. More on how this works will be detailed in #Order-Of-Execution.

```public class CustomEvent extends Event implements Cancellable {

private boolean cancelled = false;

public String string;

publicd CustomEvent(String string) {
    this.string = string;
}

public boolean isCancelled() {
    return this.cancelled;
}

public void setCancelled(boolean cancelled) {
    this.cancelled = cancelled;
}
```

Having an event as cancellable means you can skip over some, or the rest of the event handlers as needed. Again, more information on how all this works will be detailed in #Order-Of-Execution.

Creating EventHandlers utilizes annotations and reflections so that using this anywhere is possible and clean. To create an EventHandler, simple add the annotation `@EventHandler` above the method that should be handling the event. (Note that there are specific requirements for an EventHandler to be registered. EventHandlers MUST have one parameter whose type is assignable from the Event class, or is the Event class.)

```
@EventHandler(execution = EventExecution.MIDDLE, ignoreCancelled = false)
public void sampleListener(CustomEvent event) {
    System.out.println("Custom event called! Event string: " + event.string);
}
```

You'll probably notice the annotation parameters `execution` and `ignoreCancelled`. These have to do with the order of execution. More info on that is detialed in #Order-Of-Execution. However, these parameters are optional. Their defaults are shown above in the example.

Registering event listeners (eventhandlers) is pretty easy as well. EventHandlers must reside in a class that implements Listener (`public class CustomListener implements Listener {`). You should then use an instance of that class to register its EventHandlers or to unregister them. Please note that you must use the same instance to unregister EventHandlers.

```
Listener customListener = new CustomListener();`
manager.registerListeners(customListener);
manager.unregisterListeners(customListener);
```
That's pretty much it for using the EventAPI. More information on the order of execution is shown below.

# Order Of Execution

The order of execution for EventHandlers is meant to be as straight-forward as possible, but there are some exceptions and details that you might want to take note of. 

To start, when running the #call(Event) method in the EventManager, only listeners registered in that EventManager will be used.

Next, all registered and valid EventHandlers will be gathered. EventHandlers that aren't listening for the Event class, or aren't listening for the event that is being called, are excluded. (This means EventHandlers listening to the event that is called, or listening for the Event event, will be executed.) Note that if you're listeneing for the Event event, that handler will only be called once.

After all handlers have been gathered, the order of execution begins. First, handlers listening for the Event event will be called, followed by the event that is actually being called. This acts as a pre-event-execution event.

For both the Event EventHandlers and custom event EventHandlers, the order of operation is determined by the EventHandlers EventExecution ("execution" parameter) value. These values range from, in order of, "PRE, FIRST, START, MIDDLE, END, LAST, POST, FINAL".

Calling a custom event will be executed as follows: `Event (PRE -> FINAL) -> CustomEvent (PRE -> FINAL)`

One last thing to note is the cancellation of an event. When cancelling an event, only handlers with their ignoreCancelled property as true will be executed. Until the event is uncancelled, handlers without this property as true will be skipped.

For example, lets say you had 4 handlers:
 - EventHandler-1 `@EventHandler(execution = EventExecution.PRE) public void onEvent(Event event) {event.setCancelled(true);}`
 - EventHandler-2 `@EventHandler(execution = EventExecution.PRE) public void onCustomEventPre(CustomEvent event) {/*This handler is ignored!*/}`
 - EventHandler-3 `@EventHandler(execution = EventExecution.FIRST, ignoreCancelled = true) public void onCustomEventFirst(CustomEvent event) {/*This handler is executed!*/ event.setCancelled(false);}`
 - EventHandler-4 `@EventHandler public void onCustomEvent(CustomEvent event) {/*This handler is executed!*/}`

Handler 1 is ran first because it's listening to the Event event, AND it's execution is set to PRE. Handler 1 cancels the event in order to keep other handlers from running for any given reason.

Handler 2 is not executed because Handler 1 cancelled the event, and Handler 2 isn't set to ignore if an event is cancelled or not.

Handler 3 IS executed because it is set to ignore if an event is cancelled. Therefor, all code in here is ran. In the example above, the event is uncancelled. This will allow Handler 4 to execute without requiring the ignoreCancelled property. If the event wasn't uncancelled, no other handlers without the ignoreCancelled property set to true, would run.

Handler 4 is executed because the event was uncancelled. Therefor, execution continues normally. Even if the event wasn't uncancelled, Handler 4 could still execute if it had the ignoreCancelled property set to true.

I hope the explanation and examples were understandable. If the explanation seems complicated, just follow the logic of the values and their names, and everything should make sense. Additionally, there is another form of this explanation in the documentation. Happy coding!
