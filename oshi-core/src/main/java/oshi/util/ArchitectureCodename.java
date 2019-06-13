package oshi.util;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

    /**
     *
     * Microarchitecture Codename and Processor Model and Family Numbers,
     * eg. Family 6 Model 94
     *
     */


public enum ArchitectureCodename {

    /**
     * Intel
     */

    SkyLake("6 94");

    private String familyModel;

    ArchitectureCodename(String familyModel){
        this.familyModel = familyModel;
    }

    private static final Map<String, ArchitectureCodename> lookupArchitecture =
            Stream.of(values()).collect(Collectors.toMap(Object::toString, e -> e));

    public static ArchitectureCodename getArchitecture(String familyModel){
        return lookupArchitecture.get(familyModel);
    }

}
