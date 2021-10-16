package oshi.driver.mac.disk;

import com.sun.jna.Platform;
import com.sun.jna.platform.mac.SystemB;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;


import java.util.HashMap;
import java.util.Map;


public class FsstatTest {



    @Test
    void testQueryPartitionToMountMap(){
        if(Platform.isMac()){
            int mockNumfs = 1;
            SystemB.Statfs[] mockFileSystems = new SystemB.Statfs[mockNumfs];
            SystemB.Statfs fs = new SystemB.Statfs();
            fs.f_mntfromname ="/test/dev/".getBytes();
            fs.f_mntonname = "/hello".getBytes();
            mockFileSystems[0] = fs;

            Map<String, String> expeectedMountPointMap = new HashMap<>();
            expeectedMountPointMap.put("/test","/hello");

            try(MockedStatic<Fsstat> fsstatMock = Mockito.mockStatic(Fsstat.class,Mockito.CALLS_REAL_METHODS)){
                fsstatMock.when(() -> { Fsstat.queryFsstat(null,0,0); })
                    .thenReturn(mockNumfs);
                fsstatMock.when(() -> { Fsstat.getFileSystems(mockNumfs);})
                    .thenReturn(mockFileSystems);

                Assert.assertEquals(Fsstat.queryFsstat(null,0,0),mockNumfs);
                Assert.assertEquals(Fsstat.getFileSystems(mockNumfs)[0],mockFileSystems[0]);

                Assert.assertEquals(Fsstat.queryPartitionToMountMap(), expeectedMountPointMap);
            }
        }
    }

}
