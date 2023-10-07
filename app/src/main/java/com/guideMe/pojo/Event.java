
package com.guideMe.pojo;

import java.util.ArrayList;
import java.util.List;

public class Event {
    public String id;
    public String image;
    public String title;
    public String location;
    public Long date;
    public String description;
    public List<String> usersId = new ArrayList<>();

    //
    public Boolean loved;

    public Event() {
    }

}
