package com.vtnet.netat.web.test;

import com.vtnet.netat.web.elements.Locator;
import com.vtnet.netat.web.elements.NetatUIObject;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.List;

public class Te {
    public static void main(String[] args) {

        List<Locator> locatorList = new ArrayList<>();

        Locator edtUsernameXpath = new Locator();
        edtUsernameXpath.setType(List.of("xpath"));
        edtUsernameXpath.setValue("//input[@name='username']");
        edtUsernameXpath.setActive(true);
        edtUsernameXpath.setStrategy("xpath");
        locatorList.add(edtUsernameXpath);


        Locator edtUsernameID = new Locator();
        edtUsernameID.setType(List.of("id"));
        edtUsernameID.setValue("username");
        edtUsernameID.setActive(true);
        edtUsernameID.setStrategy("id");
        locatorList.add(edtUsernameID);

        WebDriver driver = null;
//        NetatUIObject edtUsername = new NetatUIObject(locatorList);

//        edtUsername.convertToWebElement(driver).click();
//
//
//        System.out.println(edtUsername.toPrettyJson());


    }
}
