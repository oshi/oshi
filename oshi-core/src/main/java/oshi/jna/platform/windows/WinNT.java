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
     * Contains information about the relationships of logical processors and
     * related hardware. The {@link Kernel32#GetLogicalProcessorInformationEx}
     * function uses this structure.
     */
    @FieldOrder({ "relationship", "size", "payload" })
    public static class SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX extends Structure {
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
                payload.setType(AnonymousStructProcessorRelationship.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationNumaNode:
                payload.setType(AnonymousStructNumaNodeRelationship.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationCache:
                payload.setType(AnonymousStructCacheRelationship.class);
                break;
            case LOGICAL_PROCESSOR_RELATIONSHIP.RelationGroup:
                payload.setType(AnonymousStructGroupRelationship.class);
                break;
            default:
                break;
            }
            payload.read();
        }

        public static class AnonymousUnionPayload extends Union {
            /**
             * A PROCESSOR_RELATIONSHIP structure that describes processor
             * affinity. This structure contains valid data only if the
             * Relationship member is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore} or
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage}.
             */
            public AnonymousStructProcessorRelationship Processor;

            /**
             * A NUMA_NODE_RELATIONSHIP structure that describes a NUMA node.
             * This structure contains valid data only if the Relationship
             * member is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationNumaNode}.
             */
            public AnonymousStructNumaNodeRelationship NumaNode;

            /**
             * A CACHE_RELATIONSHIP structure that describes cache attributes.
             * This structure contains valid data only if the Relationship
             * member is {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationCache}.
             */
            public AnonymousStructCacheRelationship Cache;

            /**
             * A GROUP_RELATIONSHIP structure that contains information about
             * the processor groups. This structure contains valid data only if
             * the Relationship member is RelationGroup.
             */
            public AnonymousStructGroupRelationship Group;
        }

        /**
         * The PROCESSOR_RELATIONSHIP structure describes the logical processors
         * associated with either a processor core or a processor package.
         */
        @FieldOrder({ "flags", "efficiencyClass", "reserved", "groupCount", "groupMask" })
        public static class AnonymousStructProcessorRelationship extends Structure {
            /**
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorCore},
             * this member is {@code LTP_PC_SMT} if the core has more than one
             * logical processor, or 0 if the core has one logical processor.
             * <p>
             * If the Relationship member of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure is
             * {@link LOGICAL_PROCESSOR_RELATIONSHIP#RelationProcessorPackage},
             * this member is always 0.
             */
            public BYTE flags;

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
            public BYTE efficiencyClass;

            /**
             * This member is reserved.
             */
            public BYTE[] reserved = new BYTE[20];

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
            public WORD groupCount;

            /**
             * An array of {@link GROUP_AFFINITY} structures. The
             * {@link #groupCount} member specifies the number of structures in
             * the array. Each structure in the array specifies a group number
             * and processor affinity within the group.
             * <p>
             * This array will only contain a single element array when
             * instantiated. If {@link #groupCount} is greater than 1, use
             * {@link #readGroupMask(Pointer p)} to populate the larger array.
             */
            public GROUP_AFFINITY[] groupMask = new GROUP_AFFINITY[1];

            /**
             * Calculates and sets the groupMask array using the user-provided
             * pointer to find the array in native memory. Intended to be called
             * immediately after instantiation of the
             * {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX} structure that
             * this structure is a member of.
             * 
             * @param p
             *            The pointer used to instantiate the
             *            {@link SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX}
             *            structure.
             */
            public void readGroupMask(Pointer p) {
                this.groupMask = new GROUP_AFFINITY[this.groupCount.intValue()];
                // The user-provided Pointer is to the outer
                // SYSTEM_LOGICAL_PROCESSOR_INFORMATION_EX structure. Offset is:
                // Two 4-byte fields before this structure = 8 bytes
                // Two BYTE fields, 20 bytes of padding, and a WORD in this
                // structure = 24 bytes: total 32 byte offset
                for (int i = 0;i < this.groupMask.length; i++) {
                    int offset = 32 + i * Native.getNativeSize(GROUP_AFFINITY.class);
                    this.groupMask[i] = new GROUP_AFFINITY(p.share(offset));
                }
            }
        }

        @FieldOrder({ "nodeNumber", "reserved", "groupMask" })
        public static class AnonymousStructNumaNodeRelationship extends Structure {
            /**
             * Identifies the NUMA node. Valid values are {@code 0} to the
             * highest NUMA node number inclusive. A non-NUMA multiprocessor
             * system will report that all processors belong to one NUMA node.
             */
            public DWORD nodeNumber;
            /**
             * This member is reserved.
             */
            public BYTE[] reserved = new BYTE[20];
            /**
             * A {@link GROUP_AFFINITY} structure that specifies a group number
             * and processor affinity within the group.
             */
            public GROUP_AFFINITY groupMask;
        }

        @FieldOrder({ "level", "associativity", "lineSize", "cacheSize", "type", "reserved", "groupMask" })
        public static class AnonymousStructCacheRelationship extends Structure {
            /**
             * The cache level. This member can be 1 (L1), 2 (L2), or 3 (L3).
             */
            public BYTE level;
            /**
             * The cache associativity. If this member is
             * {@code CACHE_FULLY_ASSOCIATIVE (0xFF)}, the cache is fully
             * associative.
             */
            public BYTE associativity;
            /**
             * The cache line size, in bytes.
             */
            public WORD lineSize;
            /**
             * The cache size, in bytes.
             */
            public DWORD cacheSize;
            /**
             * The cache type. This member is a {@link PROCESSOR_CACHE_TYPE}
             * value.
             */
            public int /* PROCESSOR_CACHE_TYPE */ type;
            /**
             * This member is reserved.
             */
            public BYTE[] reserved = new BYTE[20];
            /**
             * A {@link GROUP_AFFINITY} structure that specifies a group number
             * and processor affinity within the group.
             */
            public GROUP_AFFINITY groupMask;
        }

        @FieldOrder({ "maximumGroupCount", "activeGroupCount", "reserved", "groupInfo" })
        public static class AnonymousStructGroupRelationship extends Structure {
            /**
             * The maximum number of processor groups on the system.
             */
            public WORD maximumGroupCount;
            /**
             * The number of active groups on the system. This member indicates
             * the number of {@link PROCESSOR_GROUP_INFO} structures in the
             * GroupInfo array.
             */
            public WORD activeGroupCount;
            /**
             * This member is reserved.
             */
            public BYTE[] reserved = new BYTE[20];
            /**
             * An array of {@link PROCESSOR_GROUP_INFO} structures. Each
             * structure represents the number and affinity of processors in an
             * active group on the system.
             * <p>
             * To retrieve this array, use {@link getGroupInfo()}
             */
            public Pointer groupInfo;

            /**
             * Accessor method for the {@link groupInfo} member.
             * 
             * @return An array of {@link PROCESSOR_GROUP_INFO} structures.
             */
            public PROCESSOR_GROUP_INFO[] getGroupInfo() {
                Pointer[] array = groupInfo.getPointerArray(0L, activeGroupCount.intValue());
                PROCESSOR_GROUP_INFO[] groups = new PROCESSOR_GROUP_INFO[array.length];
                for (int i = 0; i < groups.length; i++) {
                    groups[i] = new PROCESSOR_GROUP_INFO(array[i]);
                }
                return groups;
            }
        }

        @FieldOrder({ "mask", "group", "reserved" })
        public static class GROUP_AFFINITY extends Structure {
            public ULONG_PTR /* KAFFINITY */ mask;
            public WORD group;
            public WORD[] reserved = new WORD[3];

            public GROUP_AFFINITY(Pointer memory) {
                super(memory);
                read();
            }

            public GROUP_AFFINITY() {
                super();
            }
        }

        @FieldOrder({ "maximumProcessorCount", "activeProcessorCount", "reserved", "activeProcessorMask" })
        public static class PROCESSOR_GROUP_INFO extends Structure {
            BYTE maximumProcessorCount;
            BYTE activeProcessorCount;
            BYTE[] reserved = new BYTE[38];
            ULONG_PTR /* KAFFINITY */ activeProcessorMask;

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
