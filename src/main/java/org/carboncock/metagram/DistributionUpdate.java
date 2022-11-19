package org.carboncock.metagram;

import org.carboncock.metagram.annotation.EventHandler;
import org.carboncock.metagram.annotation.handle.HandleAny;
import org.carboncock.metagram.annotation.handle.HandleCallback;
import org.carboncock.metagram.annotation.handle.HandleCommand;
import org.carboncock.metagram.exception.RegisterException;
import org.carboncock.metagram.exception.UpdateProcessException;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements the delivery of updates.
 *
 * @author Pavel Sharaev (mail@pechhenka.ru)
 */
public class DistributionUpdate {
    private final List<MethodOnClass> handlersCallback = new ArrayList<>();
    private final List<MethodOnClass> handlersCommand = new ArrayList<>();
    private final List<MethodOnClass> handlersAny = new ArrayList<>();

    private static void addIfExistAnnotation(final List<MethodOnClass> list, final Object eventHandler, final Method method, final Class<? extends Annotation> annotationClass) {
        final var annotation = method.getAnnotation(annotationClass);
        if (annotation != null) {
            list.add(new MethodOnClass(eventHandler, method));
        }
    }

    public <T extends TelegramLongPollingBot> void deliverUpdate(final T bot, final Update update) throws UpdateProcessException {
        processAny(bot, update);
        if (update.hasCallbackQuery()) {
            processCallback(bot, update);
        } else if (update.hasMessage()) {
            final Message message = update.getMessage();
            if (message.isCommand()) {
                processCommand(bot, update);
            }
        }
    }

    public void registerEventHandlers(final String packagePath) throws RegisterException {
        final var s = new Reflections(packagePath, Scanners.SubTypes.filterResultsBy(ignored -> true)).getSubTypesOf(Object.class);
        RegisterException exception = null;
        for (final var k : s) {
            try {
                if (!k.isAnnotationPresent(EventHandler.class)) {
                    continue;
                }
                final var o = k.getDeclaredConstructor().newInstance();
                registerEventHandler(o);
            } catch (final Exception e) {
                if (exception == null) {
                    exception = new RegisterException("Failed register event handler", e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    public void registerEventHandler(final Object eventHandler) throws RegisterException {
        final var clazz = eventHandler.getClass();

        if (clazz.getAnnotation(EventHandler.class) == null) {
            throw new RegisterException("Not have EventHandler annotation: " + clazz.getCanonicalName());
        }

        final var methods = clazz.getDeclaredMethods();

        for (final var method : methods) {
            final var args = method.getParameterTypes();
            if (args.length != 2) {
                continue;
            }
            if (!args[0].isAssignableFrom(TelegramLongPollingBot.class)) {
                continue;
            }
            if (!args[1].equals(Update.class)) {
                continue;
            }

            addIfExistAnnotation(handlersAny, eventHandler, method, HandleAny.class);
            addIfExistAnnotation(handlersCallback, eventHandler, method, HandleCallback.class);
            addIfExistAnnotation(handlersCommand, eventHandler, method, HandleCommand.class);
        }
    }

    private <T extends TelegramLongPollingBot> void processAny(final T bot, final Update update) throws UpdateProcessException {
        UpdateProcessException exception = null;
        for (final var m : handlersAny) {
            try {
                m.invoke(bot, update);
            } catch (final Exception e) {
                if (exception == null) {
                    exception = new UpdateProcessException("Failed process callback", e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private <T extends TelegramLongPollingBot> void processCallback(final T bot, final Update update) throws UpdateProcessException {
        final var callback = update.getCallbackQuery();
        final var data = callback.getData();
        UpdateProcessException exception = null;
        for (final var m : handlersCallback) {
            try {
                final var callbackAnnotation = m.getMethodAnnotation(HandleCallback.class);
                assert callbackAnnotation != null;
                final var name = callbackAnnotation.value();
                final var selector = callbackAnnotation.selector();
                switch (selector) {
                    case EQUALS -> {
                        if (data.equals(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case EQUALS_IGNORE_CASE -> {
                        if (data.equalsIgnoreCase(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case START_WITH -> {
                        if (data.startsWith(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case CONTAINS -> {
                        if (data.contains(name)) {
                            m.invoke(bot, update);
                        }
                    }
                }
            } catch (final Exception e) {
                if (exception == null) {
                    exception = new UpdateProcessException("Failed process callback", e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private <T extends TelegramLongPollingBot> void processCommand(final T bot, final Update update) throws UpdateProcessException {
        final var message = update.getMessage();
        final var text = message.getText();
        UpdateProcessException exception = null;
        for (final var m : handlersCommand) {
            try {
                final var commandAnnotation = m.getMethodAnnotation(HandleCommand.class);
                assert commandAnnotation != null;
                final var name = commandAnnotation.value();
                final var selector = commandAnnotation.selector();
                switch (selector) {
                    case EQUALS -> {
                        if (text.equals(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case EQUALS_IGNORE_CASE -> {
                        if (text.equalsIgnoreCase(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case START_WITH -> {
                        if (text.startsWith(name)) {
                            m.invoke(bot, update);
                        }
                    }
                    case CONTAINS -> {
                        if (text.contains(name)) {
                            m.invoke(bot, update);
                        }
                    }
                }
            } catch (final Exception e) {
                if (exception == null) {
                    exception = new UpdateProcessException("Failed process callback", e);
                } else {
                    exception.addSuppressed(e);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private record MethodOnClass(Object o, Method method) {
        public void invoke(final Object... args) throws InvocationTargetException, IllegalAccessException {
            method.invoke(o, args);
        }

        public <T extends Annotation> T getMethodAnnotation(final Class<T> annotationClass) {
            return method.getAnnotation(annotationClass);
        }
    }
}
