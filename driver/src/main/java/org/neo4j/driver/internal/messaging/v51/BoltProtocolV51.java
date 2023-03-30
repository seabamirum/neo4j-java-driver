/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.messaging.v51;

import static org.neo4j.driver.internal.async.connection.ChannelAttributes.messageDispatcher;

import io.netty.channel.ChannelPromise;
import java.util.Collections;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.NotificationConfig;
import org.neo4j.driver.internal.cluster.RoutingContext;
import org.neo4j.driver.internal.handlers.HelloResponseHandler;
import org.neo4j.driver.internal.handlers.LogonResponseHandler;
import org.neo4j.driver.internal.messaging.BoltProtocol;
import org.neo4j.driver.internal.messaging.BoltProtocolVersion;
import org.neo4j.driver.internal.messaging.MessageFormat;
import org.neo4j.driver.internal.messaging.request.HelloMessage;
import org.neo4j.driver.internal.messaging.request.LogonMessage;
import org.neo4j.driver.internal.messaging.v5.BoltProtocolV5;
import org.neo4j.driver.internal.security.InternalAuthToken;

public class BoltProtocolV51 extends BoltProtocolV5 {
    public static final BoltProtocolVersion VERSION = new BoltProtocolVersion(5, 1);
    public static final BoltProtocol INSTANCE = new BoltProtocolV51();

    @Override
    public void initializeChannel(
            String userAgent,
            AuthToken authToken,
            RoutingContext routingContext,
            ChannelPromise channelInitializedPromise,
            NotificationConfig notificationConfig) {
        var exception = verifyNotificationConfigSupported(notificationConfig);
        if (exception != null) {
            channelInitializedPromise.setFailure(exception);
            return;
        }
        var channel = channelInitializedPromise.channel();
        HelloMessage message;

        if (routingContext.isServerRoutingEnabled()) {
            message = new HelloMessage(
                    userAgent, Collections.emptyMap(), routingContext.toMap(), false, notificationConfig);
        } else {
            message = new HelloMessage(userAgent, Collections.emptyMap(), null, false, notificationConfig);
        }

        messageDispatcher(channel).enqueue(new HelloResponseHandler(channel.voidPromise()));
        messageDispatcher(channel).enqueue(new LogonResponseHandler(channelInitializedPromise));
        channel.write(message, channel.voidPromise());
        channel.writeAndFlush(new LogonMessage(((InternalAuthToken) authToken).toMap()));
    }

    @Override
    public BoltProtocolVersion version() {
        return VERSION;
    }

    @Override
    public MessageFormat createMessageFormat() {
        return new MessageFormatV51();
    }
}