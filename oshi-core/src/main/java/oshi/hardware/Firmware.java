package oshi.hardware;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: SchiTho1
 * Date: 14.10.2016
 * Time: 07:53
 * <p>
 * Copyright 2008-2013 - Securiton AG all rights reserved
 */
public interface Firmware {

    String getManufacturer();

    String getName();

    String getDescription();

    String getVersion();

    Date getReleaseDate();
}
