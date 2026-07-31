package com.mzinx.mongodb.messaging.command;

import java.util.Map;

import org.bson.Document;
import org.springframework.stereotype.Component;

import com.mzinx.mongodb.messaging.config.MessagingProperties;
import com.mzinx.mongodb.messaging.model.Message;

/**
 * Factory for command {@link Message}s broadcast to the command destination
 * (see {@code messaging.command-path}, default {@code /cmd}).
 * <p>
 * Command messages all share the shape
 * <pre>{ "type": "&lt;TYPE&gt;", ...fields }</pre>
 * carried in {@link Message#getContent()}. This helper centralises that
 * convention so callers never hand-build the {@code type} field or the target
 * destination.
 * <p>
 * As a Spring {@link Component} it defaults the command destination to
 * {@link MessagingProperties#getCommandPath()} — the instance methods
 * ({@link #command(String, Document)}, {@link #refresh(String)}) require no path
 * argument. Static overloads accepting an explicit {@code commandPath} remain
 * available for callers that need to target a different destination.
 * <p>
 * The library ships the generic {@link #COMMAND_TYPE_KEY type key} constant and
 * the {@link Type#REFRESH REFRESH} command it emits itself; applications add
 * their own command types (e.g. presence, private-channel) on top of the same
 * builder.
 */
@Component
public class CommandMessages {

    /** The field in a command's content that names the command type. */
    public static final String COMMAND_TYPE_KEY = "type";

    /** Command types emitted by the message-queuing library itself. */
    public static final class Type {
        /** Tells clients a watched collection changed and its view should refresh. */
        public static final String REFRESH = "REFRESH";

        private Type() {
        }
    }

    private final MessagingProperties messagingProperties;

    public CommandMessages(MessagingProperties messagingProperties) {
        this.messagingProperties = messagingProperties;
    }

    // ---------------------------------------------------------------------
    // Instance API — command destination defaults to messaging.command-path.
    // ---------------------------------------------------------------------

    /** The configured command destination (default {@code /cmd}). */
    public String commandPath() {
        return messagingProperties.getCommandPath();
    }

    /**
     * Builds a command targeted at the configured command destination whose
     * content is {@code {type: <type>, ...extra}}.
     */
    public Message command(String type, Document extra) {
        return command(commandPath(), type, extra);
    }

    /** Convenience overload taking the extra content fields as a map. */
    public Message command(String type, Map<String, ?> extra) {
        return command(commandPath(), type, extra);
    }

    /**
     * Builds a {@link Type#REFRESH REFRESH} command (targeted at the configured
     * command destination) instructing clients to refresh the view backed by
     * {@code collection}.
     */
    public Message refresh(String collection) {
        return refresh(commandPath(), collection);
    }

    // ---------------------------------------------------------------------
    // Static API — explicit command destination.
    // ---------------------------------------------------------------------

    /**
     * Builds a command {@link Message} targeted at {@code commandPath} whose
     * content is {@code {type: <type>, ...extra}}.
     *
     * @param commandPath the command destination
     * @param type        the command type placed under {@link #COMMAND_TYPE_KEY}
     * @param extra       additional content fields (may be {@code null}/empty)
     */
    public static Message command(String commandPath, String type, Document extra) {
        Document content = new Document(COMMAND_TYPE_KEY, type);
        if (extra != null) {
            content.putAll(extra);
        }
        return Message.builder().target(commandPath).content(content).build();
    }

    /** Convenience overload taking the extra content fields as a map. */
    public static Message command(String commandPath, String type, Map<String, ?> extra) {
        return command(commandPath, type, extra != null ? new Document(extra) : null);
    }

    /**
     * Builds a {@link Type#REFRESH REFRESH} command instructing clients to
     * refresh the view backed by {@code collection}.
     *
     * @param commandPath the command destination
     * @param collection  the collection whose data changed (placed under {@code coll})
     */
    public static Message refresh(String commandPath, String collection) {
        return command(commandPath, Type.REFRESH, new Document("coll", collection));
    }
}
