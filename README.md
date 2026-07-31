# MongoDB Spring Message Queuing

A Spring Boot starter library that provides message queuing and real-time data synchronization capabilities using MongoDB as the message store and change streams for live updates. It combines WebSocket communication with MongoDB's change streams to enable real-time messaging and live data features.

## Features

- **Message Queuing**: Persistent message storage with TTL-based automatic cleanup
- **Real-time Messaging**: WebSocket-based instant message delivery using STOMP
- **Live Data Synchronization**: Automatic propagation of database changes to connected clients
- **Change Stream Integration**: Leverages MongoDB change streams for real-time data updates
- **Configurable Endpoints**: Customizable WebSocket endpoints and message paths
- **TTL Message Management**: Automatic expiration of old messages
- **STOMP Heartbeats**: Server-side heartbeats (on by default) so dead connections — including intermittent network drops — are detected within seconds
- **Spring Integration**: Seamless integration with Spring Boot and WebSocket

## Installation

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.mzinx</groupId>
    <artifactId>mongodb-spring-message-queuing</artifactId>
    <version>1.0.0</version>
</dependency>
```

Also add the change stream dependency for live data features:

```xml
<dependency>
    <groupId>com.mzinx</groupId>
    <artifactId>mongodb-spring-change-stream</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Configuration

Configure the messaging service using the following properties in your `application.properties` or `application.yml`:

```properties
# Enable/disable messaging functionality (default: true)
messaging.enabled=true

# MongoDB collection for storing messages (default: _messages)
messaging.collection=_messages

# WebSocket endpoint path (default: /ws)
messaging.webSocketEndpoint=/ws

# Push message endpoint (default: /push)
messaging.pushPath=/push

# Command message endpoint (default: /cmd)
messaging.commandPath=/cmd

# Synchronization message endpoint (default: /sync)
messaging.syncPath=/sync

# Maximum lifetime for messages in milliseconds (default: 86400000 = 24 hours)
messaging.maxLifeTime=86400000

# Collections to watch for live data changes (comma-separated list)
messaging.watchCollections=users,orders,products

# STOMP broker heartbeats (default: 10000 ms each direction). Heartbeats let the
# server detect a dead connection — e.g. an intermittent network drop where no
# clean close is received — within ~one interval, so a WebSocket
# SessionDisconnectEvent fires promptly instead of waiting on OS-level TCP
# timeouts. Set either to 0 to disable that direction (both 0 disables heartbeats).
messaging.heartbeat.server-ms=10000
messaging.heartbeat.client-ms=10000
```

> **Heartbeat scheduler:** when heartbeats are enabled the library provides a
> single-thread `TaskScheduler` bean named `messagingHeartbeatScheduler`. Define
> your own bean of that name to use an existing scheduler instead. Client STOMP
> libraries must also advertise matching heartbeats (e.g. stomp.js defaults to
> 10 s/10 s) for the server to receive client-to-server pings.

## Usage

### Basic Message Queuing

#### Client-side (JavaScript/WebSocket)

```javascript
// Connect to WebSocket
const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    
    // Subscribe to command messages
    stompClient.subscribe('/cmd', function(message) {
        console.log('Received command:', JSON.parse(message.body));
    });
    
    // Subscribe to sync messages
    stompClient.subscribe('/sync', function(message) {
        console.log('Received sync:', JSON.parse(message.body));
    });
    
    // Send a message
    stompClient.send('/app/push', {}, JSON.stringify({
        target: '/cmd',
        content: { type: 'NOTIFICATION', message: 'Hello World' }
    }));
});
```

### Live Data Synchronization

Configure collections to watch for changes:

```properties
messaging.watchCollections=users,orders,inventory
```

When documents in these collections are modified, the changes are automatically broadcast to connected clients:

```javascript
// Subscribe to live data updates
stompClient.subscribe('/sync', function(message) {
    const update = JSON.parse(message.body);
    console.log('Live update:', update);
    
    // Handle different operation types
    switch(update.operationType) {
        case 'INSERT':
        case 'REPLACE':
            // Add/update item in UI
            break;
        case 'UPDATE':
            // Update specific fields
            break;
        case 'DELETE':
            // Remove item from UI
            break;
    }
});
```

## Architecture

### Message Flow

1. **Client** sends message via WebSocket to `/push`
2. **MessageController** receives message and calls `messageService.queue()`
3. **MessageService** saves message to MongoDB with TTL
4. **Change Stream** detects new message insertion
5. **MessageService** sends message to WebSocket subscribers
6. **Client** receives message via subscribed endpoints

### Live Data Flow

1. **Database** change occurs in watched collection
2. **Change Stream** detects the change
3. **LiveDataService** processes the change event
4. **MessageService** sends update to WebSocket subscribers
5. **Client** receives real-time update

## WebSocket Endpoints

- **`/ws`** - Main WebSocket endpoint (SockJS fallback available)
- **`/push`** - Endpoint for sending messages to queue
- **`/cmd`** - Subscription endpoint for command messages
- **`/sync`** - Subscription endpoint for synchronization messages

## Message Persistence

Messages are stored in MongoDB with automatic TTL-based cleanup:

- Messages expire after `messaging.maxLifeTime` milliseconds
- TTL index ensures automatic cleanup of old messages
- Failed deliveries can be retried from persistent storage

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.
