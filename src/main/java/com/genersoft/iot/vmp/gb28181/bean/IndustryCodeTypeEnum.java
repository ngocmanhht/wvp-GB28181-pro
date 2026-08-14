package com.genersoft.iot.vmp.gb28181.bean;

import lombok.Getter;

/**
 * Include industry codes
 */
public enum IndustryCodeTypeEnum {
    SOCIAL_SECURITY_ROAD("00", "Social security road access", "Including urban pavements, commercial streets, public areas, and key areas"),
    SOCIAL_SECURITY_COMMUNITY("01", "Social security community access", "Including communities, buildings, Internet cafes, etc."),
    SOCIAL_SECURITY__INTERNAL("02", "Internal access to social security ", "Including public security office building, detention room, etc."),
    SOCIAL_SECURITY_OTHER("03", "Other access to social security", ""),
    TRAFFIC_ROAD("04", "Traffic road access ", "Including monitoring of traffic conditions on major urban arterial roads, national highways, and highways"),
    TRAFFIC_BAYONET("05", "Traffic checkpoint access", "Including intersections, "electronic police", checkpoints, toll stations, etc."),
    TRAFFIC_INTERNAL("06", "Traffic internal access", "Including traffic control office building, etc."),
    TRAFFIC_OTHER("07", "Transportation other access", ""),
    CITY_MANAGEMENT("08", "City management access", ""),
    HEALTH_ENVIRONMENTAL_PROTECTION("09", "Health and environmental protection access", ""),
    COMMODITY_INSPECTION_CUSTOMHOUSE("10", "Commodity inspection and customs access", ""),
    EDUCATION_SECTOR("11", "Education sector access", ""),
    CIVIL_AVIATION("12", "Civil aviation access", ""),
    RAILWAY("13", "rail access", ""),
    SHIPPING("14", "shipping access", ""),
    AGRICULTURE_FORESTRY_ANIMAL_HUSBANDRY_FISHING("40", "Agriculture, forestry, animal husbandry and fishery access", ""),
    MINING("41", "Mining industry access", ""),
    MANUFACTURING_INDUSTRY("42", "Manufacturing access", ""),
    ELECTRICITY_HEAT_GAS_AND_WATER_PRODUCTION_AND_SUPPLY("43", "Access to electricity, heat, gas and water production and supply industries", ""),
    CONSTRUCTION("44", "Construction industry access", ""),
    WHOLESALE_AND_RETAIL("45", "Wholesale and retail access", ""),
    ;

    /**
     * Access type code
     */
    @Getter
    private String name;

    /**
     * Name
     */
    @Getter
    private String code;

    /**
     * Remarks
     */
    @Getter
    private String notes;

    IndustryCodeTypeEnum(String code, String name, String notes) {
        this.name = name;
        this.code = code;
        this.notes = notes;
    }
}
