package it.yteicos.toolcupboards.api.block;

/**
 * The role a build block plays in a base.
 */
public enum BuildBlockType {

    /**
     * Ground-level block. Must rest on the world, may carry {@link #STRUCTURE} blocks and tool
     * cupboards on top of it.
     */
    FOUNDATION,

    /**
     * Wall/ceiling block. May only be placed on top of a {@link #FOUNDATION} block or of another
     * structure block that ultimately rests on one.
     */
    STRUCTURE;

    /**
     * @return {@code true} if a tool cupboard is allowed to be placed directly on this type
     */
    public boolean supportsCupboard() {
        return this == FOUNDATION;
    }
}
