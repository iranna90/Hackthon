package com.irdeto.hackthon;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DataDto {
    private final int itemID;

    @JsonCreator
    public DataDto(@JsonProperty("itemID") int itemID) {
        this.itemID = itemID;
    }

    public int getItemID() {
        return itemID;
    }

    @Override
    public String toString() {
        return "DataDto{" +
                "itemID=" + itemID +
                '}';
    }
}
