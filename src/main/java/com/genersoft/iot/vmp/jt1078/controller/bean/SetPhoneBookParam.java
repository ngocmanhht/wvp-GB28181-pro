package com.genersoft.iot.vmp.jt1078.controller.bean;

import com.genersoft.iot.vmp.jt1078.bean.JTPhoneBookContact;
import com.genersoft.iot.vmp.jt1078.bean.JTTextSign;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Schema(description = "Set up phone book")
public class SetPhoneBookParam {

    @Schema(description = "Terminal mobile phone number")
    private String phoneNumber;

    @Schema(description = "Setting type:\n" +
            "0: Delete all contacts stored on the terminal,\n" +
            "1: Indicates updating the phone book, deleting all contacts in the terminal and appending contacts in messages.,\n" +
            "2: Indicates adding a phone book,\n" +
            "3: Indicates modifying the phone book $ with contacts as the index")
    private int type;

    @Schema(description = "Contact person")
    private List<JTPhoneBookContact> phoneBookContactList;

    @Override
    public String toString() {
        return "SetPhoneBookParam{" +
                "Device mobile phone number='" + phoneNumber + '\'' +
                ", type=" + type +
                ", phoneBookContactList=" + phoneBookContactList +
                '}';
    }
}
