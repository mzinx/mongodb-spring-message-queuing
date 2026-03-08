package com.mzinx.mongodb.messaging.model;

import java.util.Date;

import org.bson.codecs.pojo.annotations.BsonProperty;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@messagingProperties.collection}")
public class Message {
    public enum Type {
        REQ, ACK, RES
    }
    private Type type;

    @Field("t")
    @BsonProperty("t")
    private String target;

    @Field("c")
    @BsonProperty("c")
    private org.bson.Document content;

    @Field("cAt")
    @BsonProperty("cAt")
    private Date createdAt;
}
