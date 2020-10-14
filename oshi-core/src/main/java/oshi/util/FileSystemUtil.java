/**
 * MIT License
 *
 * Copyright (c) 2010 - 2020 The OSHI Project Contributors: https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.util;

import org.reflections.Reflections;
import oshi.software.common.OSFileStoreInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FileSystemUtil {

    private FileSystemUtil() {
    }

    public static <T extends OSFileStoreInterface> Map<String, T> getSpecialFileSystems() {
        Map<String, T> specialFileSystems = new HashMap<>();

        Reflections reflections = new Reflections("firstdeveloper.examples.reflections");
        Set<Class<? extends OSFileStoreInterface>> classes = reflections.getSubTypesOf(OSFileStoreInterface.class);

        classes.forEach(clazz -> {
            try {
                specialFileSystems.put(clazz.getName(), (T) clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        });

        return specialFileSystems;
    }

}
