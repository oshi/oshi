/**
 * OSHI (https://github.com/oshi/oshi)
 *
 * Copyright (c) 2010 - 2019 The OSHI Project Team:
 * https://github.com/oshi/oshi/graphs/contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oshi.jna.platform.windows;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.Structure.FieldOrder;
import com.sun.jna.Union;

/**
 * WinNT.
 */
public interface WinNT extends com.sun.jna.platform.win32.WinNT {

    /**
     * Flag identifying hyperthreading / simultaneous multithreading (SMT)
     */
    int LTP_PC_SMT = 0x1;

    /**
     * Contains information about the relationships of logical processors and
     * related hardware. The {@link Kernel32#GetLogicalProcessorInformationEx}
     * function uses this structure.
     */
    @FieldOrder({ "relationship", "size", "payload" })
    class SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX extends Structure {
        /**
         * The type of relationship between the logical processors. This
         * parameter can be one of the following values:
         * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationCache},
         * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationGroup},
         * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationNumaNode},
         * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore} or
         * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage}.
         */
        public int /* LOGICAL_PROCESSOR_RELATIONSHIP */ relationship;

        /**
         * The size of the structure.
         */
        public int size;

        /**
         * A union of fields which differs depending on {@link #relationship}.
         * The type is either {@link PROCESSOR_RELATIONSHIP},
         * {@link NUMA_NODE_RELATIONSHIP}, {@link CACHE_RELATIONSHIP}, or
         * {@link GROUP_RELATIONSHIP}.
         */
        public AnonymousUnionPayload payload;

        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX() {
            super();
        }

        public SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX(Pointer memory) {
            super(memory);
            read();
        }

        @Override
        public void read() {
            super.read();
            switch (relationship) {
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorCore:
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationProcessorPackage:
                payload.setType(PROCESSOR_RELATIONSHIP.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                payload.setType(NUMA_NODE_RELATIONSHIP.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                payload.setType(CACHE_RELATIONSHIP.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                payload.setType(GROUP_RELATIONSHIP.class);
                break;
            default:
                break;
            }
            payload.read();
        }

        public static class AnonymousUnionPayload extends Union {

            /**
             * JNA requires allocated memory for the largest structure in a
             * Union. This padding value calculates the minimum extra Java-side
             * allocation required beyond native memory requirements.
             */
            public static final int UNION_ALLOCATION_PADDING;
            static {
                int proc = new PROCESSOR_RELATIONSHIP().size();
                int numa = new NUMA_NODE_RELATIONSHIP().size();
                int cache = new CACHE_RELATIONSHIP().size();
                int group = new GROUP_RELATIONSHIP().size();
                UNION_ALLOCATION_PADDING = Math.max(Math.max(proc, numa), Math.max(cache, group))
                        - Math.min(Math.min(proc, numa), Math.min(cache, group));
            }
            /**
             * A PROCESSOR_RELATIONSHIP structure that describes processor
             * affinity. This structure contains valid data only if the
             * Relationship member is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore} or
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage}.
             */
            public PROCESSOR_RELATIONSHIP Processor;

            /**
             * A NUMA_NODE_RELATIONSHIP structure that describes a NUMA node.
             * This structure contains valid data only if the Relationship
             * member is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationNumaNode}.
             */
            public NUMA_NODE_RELATIONSHIP NumaNode;

            /**
             * A CACHE_RELATIONSHIP structure that describes cache attributes.
             * This structure contains valid data only if the Relationship
             * member is {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationCache}.
             */
            public CACHE_RELATIONSHIP Cache;

            /**
             * A GROUP_RELATIONSHIP structure that contains information about
             * the processor groups. This structure contains valid data only if
             * the Relationship member is RelationGroup.
             */
            public GROUP_RELATIONSHIP Group;
        }

        /**
         * The PROCESSOR_RELATIONSHIP structure describes the logical processors
         * associated with either a processor core or a processor package.
         */
        @FieldOrder({ "flags", "efficiencyClass", "reserved", "groupCount", "groupMask" })
        public static class PROCESSOR_RELATIONSHIP extends Structure {
            /**
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore},
             * this member is {@link #LTP_PC_SMT} if the core has more than one
             * logical processor, or 0 if the core has one logical processor.
             * <p>
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage},
             * this member is always 0.
             */
            public byte flags;

            /**
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore},
             * EfficiencyClass specifies the intrinsic tradeoff between
             * performance and power for the applicable core. A core with a
             * higher value for the efficiency class has intrinsically greater
             * performance and less efficiency than a core with a lower value
             * for the efficiency class. EfficiencyClass is only nonzero on
             * systems with a heterogeneous set of cores.
             * <p>
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage},
             * EfficiencyClass is always 0.
             * <p>
             * The minimum operating system version that supports this member is
             * Windows 10.
             */
            public byte efficiencyClass;

            /**
             * This member is reserved.
             */
            public byte[] reserved = new byte[20];

            /**
             * This member specifies the number of entries in the GroupMask
             * array.
             * <p>
             * If the PROCESSOR_RELATIONSHIP structure represents a processor
             * core, the GroupCount member is always 1.
             * <p>
             * If the PROCESSOR_RELATIONSHIP structure represents a processor
             * package, the GroupCount member is 1 only if all processors are in
             * the same processor group. If the package contains more than one
             * NUMA node, the system might assign different NUMA nodes to
             * different processor groups. In this case, the GroupCount member
             * is the number of groups to which NUMA nodes in the package are
             * assigned.
             */
            public short groupCount;

            /**
             * An array of {@link GROUP_AFFINITY} structures. The
             * {@link #groupCount} member specifies the number of structures in
             * the array. Each structure in the array specifies a group number
             * and processor affinity within the group.
             * <p>
             * This Pointer is a placeholder. Use {@link #getGroupMask()} to
             * return the array.
             */
            public Pointer groupMask;

            /**
             * @return An array of {@link GROUP_AFFINITY} structures. The
             *         {@link #groupCount} member specifies the number of
             *         structures in the array. Each structure in the array
             *         specifies a group number and processor affinity within
             *         the group.
             */
            public GROUP_AFFINITY[] getGroupMask() {
                // Get pointer to array in memory
                int baseOffset = this.fieldOffset("groupMask");
                // Instantiate new array
                GROUP_AFFINITY[] mask = new GROUP_AFFINITY[this.groupCount];
                for (int i = 0; i < mask.length; i++) {
                    int offset = baseOffset + i * Native.getNativeSize(GROUP_AFFINITY.class);
                    mask[i] = new GROUP_AFFINITY(this.getPointer().share(offset));
                }
                return mask;
            }
        }

        /**
         * Represents information about a NUMA node in a processor group.
         */
        @FieldOrder({ "nodeNumber", "reserved", "groupMask" })
        public static class NUMA_NODE_RELATIONSHIP extends Structure {
            /**
             * Identifies the NUMA node. Valid values are {@code 0} to the
             * highest NUMA node number inclusive. A non-NUMA multiprocessor
             * system will report that all processors belong to one NUMA node.
             */
            public int nodeNumber;
            /**
             * This member is reserved.
             */
            public byte[] reserved = new byte[20];
            /**
             * A {@link GROUP_AFFINITY} structure that specifies a group number
             * and processor affinity within the group.
             */
            public GROUP_AFFINITY groupMask;
        }

        /**
         * Describes cache attributes.
         */
        @FieldOrder({ "level", "associativity", "lineSize", "cacheSize", "type", "reserved", "groupMask" })
        public static class CACHE_RELATIONSHIP extends Structure {
            /**
             * The cache level. This member can be 1 (L1), 2 (L2), or 3 (L3).
             */
            public byte level;
            /**
             * The cache associativity. If this member is
             * {@link #CACHE_FULLY_ASSOCIATIVE}, the cache is fully associative.
             */
            public byte associativity;
            /**
             * The cache line size, in bytes.
             */
            public short lineSize;
            /**
             * The cache size, in bytes.
             */
            public int cacheSize;
            /**
             * The cache type. This member is a {@link PROCESSOR_CACHE_TYPE}
             * value.
             */
            public int /* PROCESSOR_CACHE_TYPE */ type;
            /**
             * This member is reserved.
             */
            public byte[] reserved = new byte[20];
            /**
             * A {@link GROUP_AFFINITY} structure that specifies a group number
             * and processor affinity within the group.
             */
            public GROUP_AFFINITY groupMask;
        }

        /**
         * Represents information about processor groups.
         */
        @FieldOrder({ "maximumGroupCount", "activeGroupCount", "reserved", "groupInfo" })
        public static class GROUP_RELATIONSHIP extends Structure {
            /**
             * The maximum number of processor groups on the system.
             */
            public short maximumGroupCount;
            /**
             * The number of active groups on the system. This member indicates
             * the number of {@link PROCESSOR_GROUP_INFO} structures in the
             * GroupInfo array.
             */
            public short activeGroupCount;
            /**
             * This member is reserved.
             */
            public byte[] reserved = new byte[20];
            /**
             * An array of {@link PROCESSOR_GROUP_INFO} structures. The
             * {@link #activeGroupCount} member specifies the number of
             * structures in the array. Each structure in the array specifies
             * the number and affinity of processors in an active group on the
             * system.
             * <p>
             * This Pointer is a placeholder. Use {@link #getGroupInfo()} to
             * return the array.
             */
            public Pointer groupInfo;

            /**
             * @return An array of {@link PROCESSOR_GROUP_INFO} structures. The
             *         {@link #activeGroupCount} member specifies the number of
             *         structures in the array. Each structure in the array
             *         specifies the number and affinity of processors in an
             *         active group on the system.
             */
            public PROCESSOR_GROUP_INFO[] getGroupInfo() {
                // Get pointer to array in memory
                int baseOffset = this.fieldOffset("groupInfo");
                // Instantiate new array
                PROCESSOR_GROUP_INFO[] info = new PROCESSOR_GROUP_INFO[this.activeGroupCount];
                for (int i = 0; i < info.length; i++) {
                    int offset = baseOffset + i * Native.getNativeSize(PROCESSOR_GROUP_INFO.class);
                    info[i] = new PROCESSOR_GROUP_INFO(this.getPointer().share(offset));
                }
                return info;
            }
        }

        /**
         * Represents a processor group-specific affinity, such as the affinity
         * of a thread.
         */
        @FieldOrder({ "mask", "group", "reserved" })
        public static class GROUP_AFFINITY extends Structure {
            /**
             * A bitmap that specifies the affinity for zero or more processors
             * within the specified group.
             */
            public ULONG_PTR /* KAFFINITY */ mask;
            /**
             * The processor group number.
             */
            public short group;
            /**
             * This member is reserved.
             */
            public short[] reserved = new short[3];

            public GROUP_AFFINITY(Pointer memory) {
                super(memory);
                read();
            }

            public GROUP_AFFINITY() {
                super();
            }
        }

        /**
         * Represents the number and affinity of processors in a processor
         * group.
         */
        @FieldOrder({ "maximumProcessorCount", "activeProcessorCount", "reserved", "activeProcessorMask" })
        public static class PROCESSOR_GROUP_INFO extends Structure {
            /**
             * The maximum number of processors in the group.
             */
            public byte maximumProcessorCount;
            /**
             * The number of active processors in the group.
             */
            public byte activeProcessorCount;
            /**
             * This member is reserved.
             */
            public byte[] reserved = new byte[38];
            /**
             * A bitmap that specifies the affinity for zero or more active
             * processors within the group.
             */
            public ULONG_PTR /* KAFFINITY */ activeProcessorMask;

            public PROCESSOR_GROUP_INFO(Pointer memory) {
                super(memory);
                read();
            }

            public PROCESSOR_GROUP_INFO() {
                super();
            }
        }
    }
}
