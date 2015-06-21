/**
 * Oshi (https://github.com/dblock/oshi)
 * 
 * Copyright (c) 2010 - 2015 The Oshi Project Team
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * dblock[at]dblock[dot]org
 * alessandro[at]perucchi[dot]org
 * widdis[at]gmail[dot]com
 * https://github.com/dblock/oshi/graphs/contributors
 */
package oshi.software.os.mac.local;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;

/**
 * CoreFoundation framework for power supply stats
 * 
 * @author widdis[at]gmail[dot]com
 */
public interface CoreFoundation extends Library {
	CoreFoundation INSTANCE = (CoreFoundation) Native.loadLibrary(
			"CoreFoundation", CoreFoundation.class);

	public static final int UTF_8 = 0x08000100;

	int CFArrayGetCount(CFArrayRef array);

	CFTypeRef CFArrayGetValueAtIndex(CFArrayRef array, int index);

	void CFRelease(CFTypeRef blob);

	public class CFTypeRef extends PointerType {
		// TODO Build this out
	}

	public class CFArrayRef extends PointerType {
		// TODO Build this out
	}

	public class CFDictionaryRef extends PointerType {
		// TODO Build this out
	}

	public class CFStringRef extends PointerType {
		public static CFStringRef toCFString(String s) {
			final char[] chars = s.toCharArray();
			int length = chars.length;
			return CoreFoundation.INSTANCE.CFStringCreateWithCharacters(null,
					chars, new NativeLong(length));
		}
	}

	CFStringRef CFStringCreateWithCharacters(Object object, char[] chars,
			NativeLong length);

	boolean CFDictionaryGetValueIfPresent(CFDictionaryRef dictionary,
			CFStringRef key, PointerType value);

	Pointer CFDictionaryGetValue(CFDictionaryRef dictionary, CFStringRef key);

	boolean CFStringGetCString(Pointer foo, Pointer buffer, long maxSize,
			int encoding);

	long CFStringGetLength(Pointer str);

	long CFStringGetMaximumSizeForEncoding(long length, int encoding);

	boolean CFBooleanGetValue(Pointer booleanRef);

}
