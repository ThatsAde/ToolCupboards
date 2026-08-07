package it.yteicos.toolcupboards.api.claim;

/**
 * How a tool cupboard projects its protected volume around itself. Configured globally, or per
 * cupboard tier.
 */
public enum ClaimShape {

    /** Euclidean sphere of the configured radius. */
    SPHERE,

    /** Axis-aligned cube whose half-extent equals the configured radius. */
    CUBE
}
