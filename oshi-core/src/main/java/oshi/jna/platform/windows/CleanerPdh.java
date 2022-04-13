package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.BaseTSD.DWORD_PTR;
import com.sun.jna.platform.win32.Pdh;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.W32APIOptions;

import oshi.jna.platform.windows.WinNT.CleanerHANDLEByReference;

public interface CleanerPdh extends Pdh {
    CleanerPdh INSTANCE = Native.load("Pdh", CleanerPdh.class, W32APIOptions.DEFAULT_OPTIONS);

    int PdhOpenQuery(String szDataSource, DWORD_PTR dwUserData, CleanerHANDLEByReference phQuery);

    int PdhAddCounter(HANDLE hQuery, String szFullCounterPath, DWORD_PTR dwUserData,
            CleanerHANDLEByReference phCounter);

    int PdhAddEnglishCounter(HANDLE hQuery, String szFullCounterPath, DWORD_PTR dwUserData,
            CleanerHANDLEByReference phCounter);

}
