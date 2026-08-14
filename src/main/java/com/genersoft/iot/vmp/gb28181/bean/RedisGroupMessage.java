package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Data;

@Data
public class RedisGroupMessage {


    /**
     * Group alias
     */
    private String groupAlias;

    /**
     * Group name
     */
    private String groupName;


    /**
     * The alias of the parent group to which the group belongs
     */
    private String parentGAlias;

    /**
     * Alias of the business group to which the group belongs
     */
    private String topGroupGAlias;

    /**
     * The message type in the group change message, the value is add update delete
     */
    private String messageType;


    @Override
    public String toString() {
        return "RedisGroupMessage{" +
                ", groupAlias='" + groupAlias + '\'' +
                ", groupName='" + groupName + '\'' +
                ", parentGAlias='" + parentGAlias + '\'' +
                ", topGroupGAlias='" + topGroupGAlias + '\'' +
                '}';
    }
}
