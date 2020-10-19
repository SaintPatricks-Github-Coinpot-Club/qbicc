package cc.quarkus.qcc.type;

import java.util.Arrays;
import java.util.Objects;

/**
 *
 */
public final class CompoundType extends ValueType {
    private final Tag tag;
    private final String name;
    private final Member[] members;
    private final boolean writable;
    private final long size;
    private final int align;

    private CompoundType(final TypeSystem typeSystem, final Tag tag, final String name, final Member[] members, final long size, final int overallAlign, boolean const_, int sorted) {
        super(typeSystem, (int) ((Objects.hash((Object[]) members) * 19 + size) * 19 + Integer.numberOfTrailingZeros(overallAlign)), const_);
        // name/tag do not contribute to hash or equality
        this.tag = tag;
        this.name = name == null ? "<anon>" : name;
        this.members = members;
        // todo: assert size ≥ end of last member w/alignment etc.
        this.size = size;
        assert Integer.bitCount(overallAlign) == 1;
        this.align = overallAlign;
        boolean isWritable = ! const_;
        if (isWritable) for (Member member : members) {
            if (! member.getType().isWritable()) {
                isWritable = false;
                break;
            }
        }
        this.writable = isWritable;
    }

    CompoundType(final TypeSystem typeSystem, final Tag tag, final String name, final Member[] members, final long size, final int overallAlign, boolean const_) {
        this(typeSystem, tag, name, sort(members), size, overallAlign, const_, 0);
    }

    private static Member[] sort(Member[] array) {
        Arrays.sort(array);
        return array;
    }

    public String getName() {
        return name;
    }

    public int getMemberCount() {
        return members.length;
    }

    public Member getMember(int index) throws IndexOutOfBoundsException {
        return members[index];
    }

    public boolean isWritable() {
        return writable;
    }

    public long getSize() {
        return size;
    }

    public int getAlign() {
        return align;
    }

    ValueType constructConst() {
        return new CompoundType(typeSystem, tag, name, members, size, align, true, 0);
    }

    public CompoundType asConst() {
        return (CompoundType) super.asConst();
    }

    public boolean equals(final ValueType other) {
        return other instanceof CompoundType && equals((CompoundType) other);
    }

    public boolean equals(final CompoundType other) {
        return this == other || super.equals(other) && size == other.size && align == other.align && Arrays.deepEquals(members, other.members);
    }

    public StringBuilder toString(final StringBuilder b) {
        super.toString(b);
        b.append("compound ");
        if (tag != Tag.NONE) {
            b.append(tag).append(' ');
        }
        b.append(name).append(" {");
        if (members.length > 0) {
            Member member = members[0];
            member.toString(b);
            for (int i = 1; i < members.length; i++) {
                b.append(',');
                member.toString(b);
            }
        }
        return b.append('}');
    }

    public static final class Member implements Comparable<Member> {
        private final int hashCode;
        private final String name;
        private final ValueType type;
        private final int offset;
        private final int align;

        Member(final String name, final ValueType type, final int offset, final int align) {
            this.name = name;
            this.type = type;
            this.offset = offset;
            this.align = Math.max(align, type.getAlign());
            assert Integer.bitCount(align) == 1;
            hashCode = (Objects.hash(name, type) * 19 + offset) * 19 + Integer.numberOfTrailingZeros(align);
        }

        public String getName() {
            return name;
        }

        public ValueType getType() {
            return type;
        }

        public int getOffset() {
            return offset;
        }

        public int getAlign() {
            return align;
        }

        public int hashCode() {
            return hashCode;
        }

        public String toString() {
            return toString(new StringBuilder()).toString();
        }

        public StringBuilder toString(final StringBuilder b) {
            type.toString(b).append(' ').append(name).append('@').append(offset);
            if (align > 1) {
                b.append(" align=").append(align);
            }
            return b;
        }

        public boolean equals(final Object obj) {
            return obj instanceof Member && equals((Member) obj);
        }

        public boolean equals(final Member other) {
            return other == this || other != null && hashCode == other.hashCode && offset == other.offset && align == other.align && name.equals(other.name) && type.equals(other.type);
        }

        public int compareTo(final Member o) {
            // compare offset
            int res = Integer.compare(offset, o.offset);
            // at same offset? if so, compare size
            if (res == 0) res = Long.compare(o.type.getSize(), type.getSize());
            // at same offset *and* same size? if so, strive for *some* predictable order...
            if (res == 0) res = Integer.compare(hashCode, o.hashCode);
            return res;
        }
    }

    public enum Tag {
        NONE("untagged"),
        STRUCT("struct"),
        UNION("union"),
        ;
        private final String string;

        Tag(final String string) {
            this.string = string;
        }

        public String toString() {
            return string;
        }
    }
}