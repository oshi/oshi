package oshi.driver.mac.disk;

import com.sun.jna.Platform;
import com.sun.jna.platform.mac.SystemB;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;


import static org.mockito.Mockito.when;

public class FsstatTest {


    @Test
    void testQueryPartitionToMountMap(){
        if(Platform.isMac()){
            int mockNumfs = 1;
            SystemB.Statfs[] mockFs = new SystemB.Statfs[mockNumfs];
            SystemB.Statfs fs = new SystemB.Statfs();
            fs.f_mntfromname ="/test/dev".getBytes();
            mockFs[0] = fs;

            Fsstat fsstatSpy = Mockito.spy(new Fsstat());
            when(fsstatSpy.queryFsstat(null,0,0)).thenReturn(mockNumfs);
            when(fsstatSpy.getFileSystems(mockNumfs)).thenReturn(mockFs);

            Assert.assertEquals(mockNumfs,fsstatSpy.queryFsstat(null,0,0));
            Assert.assertEquals(mockFs[0],fsstatSpy.getFileSystems(mockNumfs)[0]);
        }
    }

}
